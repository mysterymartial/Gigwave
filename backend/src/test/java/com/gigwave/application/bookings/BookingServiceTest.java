package com.gigwave.application.bookings;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.bookings.BookingStatus;
import com.gigwave.domain.bookings.PaymentStatus;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.gigs.GigStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private GigRepository gigRepository;
    
    @InjectMocks
    private BookingService bookingService;
    
    private Gig testGig;
    private Booking testBooking;
    private UUID gigId;
    private UUID bookingId;
    private UUID musicianId;
    private UUID organizerId;
    
    @BeforeEach
    void setUp() {
        gigId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        musicianId = UUID.randomUUID();
        organizerId = UUID.randomUUID();
        
        testGig = Gig.builder()
                .id(gigId)
                .organizerId(organizerId)
                .title("Test Gig")
                .status(GigStatus.OPEN)
                .build();
        
        testBooking = Booking.builder()
                .id(bookingId)
                .gigId(gigId)
                .musicianId(musicianId)
                .bookingStatus(BookingStatus.REQUESTED)
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .acceptedAmount(new BigDecimal("75000"))
                .build();
    }
    
    @Test
    void testMusicianAcceptsGig_Success() {
        // Arrange
        BigDecimal acceptedAmount = new BigDecimal("80000");
        
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(bookingId);
            return booking;
        });
        
        // Act
        var result = bookingService.musicianAcceptsGig(gigId, musicianId, acceptedAmount);
        
        // Assert
        assertNotNull(result);
        assertEquals(acceptedAmount, result.getAcceptedAmount());
        assertEquals(BookingStatus.REQUESTED, result.getBookingStatus());
        verify(gigRepository).findById(gigId);
        verify(bookingRepository).save(any(Booking.class));
    }
    
    @Test
    void testMusicianAcceptsGig_GigNotOpen() {
        // Boundary: gig not in OPEN status
        testGig.setStatus(GigStatus.MATCHED);
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        
        assertThrows(IllegalStateException.class, 
                () -> bookingService.musicianAcceptsGig(gigId, musicianId, new BigDecimal("80000")));
    }
    
    @Test
    void testMusicianAcceptsGig_GigNotFound() {
        // Boundary: non-existent gig
        when(gigRepository.findById(gigId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.musicianAcceptsGig(gigId, musicianId, new BigDecimal("80000")));
    }
    
    @Test
    void testMusicianAcceptsGig_WithZeroAmount() {
        // Boundary: zero amount
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(bookingId);
            return booking;
        });
        
        var result = bookingService.musicianAcceptsGig(gigId, musicianId, BigDecimal.ZERO);
        
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAcceptedAmount());
    }
    
    @Test
    void testMusicianAcceptsGig_WithVeryLargeAmount() {
        // Boundary: very large amount
        BigDecimal largeAmount = new BigDecimal("999999999");
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(bookingId);
            return booking;
        });
        
        var result = bookingService.musicianAcceptsGig(gigId, musicianId, largeAmount);
        
        assertNotNull(result);
        assertTrue(result.getAcceptedAmount().compareTo(largeAmount) == 0);
    }
    
    @Test
    void testMusicianMarksDone_Success() {
        testBooking.setBookingStatus(BookingStatus.ACCEPTED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        var result = bookingService.musicianMarksDone(bookingId, musicianId);
        
        assertNotNull(result);
        assertEquals(BookingStatus.IN_PROGRESS, result.getBookingStatus());
        assertNotNull(result.getMusicianDoneAt());
        verify(bookingRepository).save(any(Booking.class));
    }
    
    @Test
    void testMusicianMarksDone_WrongMusician() {
        // Boundary: different musician tries to mark done
        UUID otherMusicianId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.musicianMarksDone(bookingId, otherMusicianId));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
    
    @Test
    void testOrganizerConfirmAndPay_Success() {
        testBooking.setBookingStatus(BookingStatus.IN_PROGRESS);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        
        var result = bookingService.organizerConfirmAndPay(bookingId, organizerId);
        
        assertNotNull(result);
        assertEquals(PaymentStatus.DEBIT_PENDING, result.getPaymentStatus());
        assertNotNull(result.getOwnerConfirmedAt());
        verify(bookingRepository).save(any(Booking.class));
    }
    
    @Test
    void testOrganizerConfirmAndPay_WrongOrganizer() {
        // Boundary: different organizer tries to confirm
        UUID otherOrganizerId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.organizerConfirmAndPay(bookingId, otherOrganizerId));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}




