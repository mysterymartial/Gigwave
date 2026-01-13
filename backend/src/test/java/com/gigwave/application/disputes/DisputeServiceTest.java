package com.gigwave.application.disputes;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.disputes.Dispute;
import com.gigwave.infrastructure.persistence.disputes.DisputeRepository;
import com.gigwave.domain.disputes.DisputeStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {
    @Mock
    private DisputeRepository disputeRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private DisputeService disputeService;
    
    private UUID bookingId;
    private UUID raisedBy;
    private UUID disputeId;
    private Booking testBooking;
    
    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        raisedBy = UUID.randomUUID();
        disputeId = UUID.randomUUID();
        
        testBooking = Booking.builder()
                .id(bookingId)
                .musicianId(raisedBy)
                .build();
    }
    
    @Test
    void testOpenDispute_Success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute dispute = invocation.getArgument(0);
            dispute.setId(disputeId);
            return dispute;
        });
        
        Dispute result = disputeService.openDispute(bookingId, raisedBy, "Test reason", "https://evidence.com");
        
        assertNotNull(result);
        assertEquals(DisputeStatus.OPEN, result.getStatus());
        verify(disputeRepository).save(any(Dispute.class));
    }
    
    @Test
    void testOpenDispute_InvalidParticipant() {
        // Boundary: non-participant tries to raise dispute
        UUID invalidUser = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        
        assertThrows(IllegalArgumentException.class,
                () -> disputeService.openDispute(bookingId, invalidUser, "Reason", null));
    }
    
    @Test
    void testOpenDispute_EmptyReason() {
        // Boundary: empty reason
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute dispute = invocation.getArgument(0);
            dispute.setId(disputeId);
            return dispute;
        });
        
        Dispute result = disputeService.openDispute(bookingId, raisedBy, "", null);
        
        assertNotNull(result);
        assertEquals("", result.getReason());
    }
    
    @Test
    void testOpenDispute_WithNullEvidence() {
        // Boundary: null evidence URL
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute dispute = invocation.getArgument(0);
            dispute.setId(disputeId);
            return dispute;
        });
        
        Dispute result = disputeService.openDispute(bookingId, raisedBy, "Reason", null);
        
        assertNotNull(result);
        assertNull(result.getEvidenceUrl());
    }
    
    @Test
    void testResolveDispute_Success() {
        Dispute dispute = Dispute.builder()
                .id(disputeId)
                .status(DisputeStatus.OPEN)
                .raisedBy(raisedBy)
                .build();
        
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(userRepository.findById(raisedBy)).thenReturn(Optional.of(new User()));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);
        
        Dispute result = disputeService.resolveDispute(disputeId, DisputeStatus.RESOLVED_MUSICIAN, UUID.randomUUID());
        
        assertEquals(DisputeStatus.RESOLVED_MUSICIAN, result.getStatus());
        assertNotNull(result.getResolvedAt());
    }
}




