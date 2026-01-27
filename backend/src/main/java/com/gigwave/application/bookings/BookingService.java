package com.gigwave.application.bookings;

import com.gigwave.api.dto.bookings.BookingDto;
import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.bookings.BookingStatus;
import com.gigwave.domain.bookings.PaymentStatus;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.gigs.GigStatus;
import com.gigwave.application.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final GigRepository gigRepository;
    private final NotificationService notificationService;

    @Transactional
    public BookingDto musicianAcceptsGig(UUID gigId, UUID musicianId, BigDecimal acceptedAmount) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));

        if (gig.getStatus() != GigStatus.OPEN) {
            throw new IllegalStateException("Gig is not open for booking");
        }

        Booking booking = Booking.builder()
                .gigId(gigId)
                .musicianId(musicianId)
                .acceptedAmount(acceptedAmount)
                .bookingStatus(BookingStatus.REQUESTED)
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .build();

        booking = bookingRepository.save(booking);
        
        // Notify organizer that a musician has bid on their gig
        notificationService.sendNewBidNotification(gig.getOrganizerId(), booking.getId(), acceptedAmount);
        
        return toDto(booking);
    }

    @Transactional
    public BookingDto organizerSelectsMusician(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        booking.setBookingStatus(BookingStatus.ACCEPTED);
        booking.setAcceptedAt(LocalDateTime.now());

        Gig gig = gigRepository.findById(booking.getGigId())
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
        gig.setStatus(GigStatus.MATCHED);

        booking = bookingRepository.save(booking);
        gigRepository.save(gig);
        
        notificationService.sendBookingAcceptedNotification(booking.getMusicianId(), bookingId);
        notificationService.sendGigAcceptedNotification(gig.getOrganizerId(), gig.getId());
        
        return toDto(booking);
    }

    @Transactional
    public BookingDto musicianMarksDone(UUID bookingId, UUID musicianId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getMusicianId().equals(musicianId)) {
            throw new IllegalArgumentException("Only the assigned musician can mark this as done");
        }

        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        booking.setMusicianDoneAt(LocalDateTime.now());

        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    @Transactional
    public BookingDto organizerConfirmAndPay(UUID bookingId, UUID organizerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        Gig gig = gigRepository.findById(booking.getGigId())
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));

        if (!gig.getOrganizerId().equals(organizerId)) {
            throw new IllegalArgumentException("Only the gig organizer can confirm payment");
        }

        booking.setOwnerConfirmedAt(LocalDateTime.now());
        booking.setPaymentStatus(PaymentStatus.DEBIT_PENDING);

        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    public BookingDto getBookingDetails(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        return toDto(booking);
    }

    public List<BookingDto> listBookingsForUser(UUID userId) {
        // Get bookings where user is musician
        List<Booking> musicianBookings = bookingRepository.findByMusicianId(userId);
        
        // Get bookings where user is organizer (via gigs)
        List<Gig> organizerGigs = gigRepository.findByOrganizerId(userId);
        List<UUID> gigIds = organizerGigs.stream().map(Gig::getId).collect(Collectors.toList());
        List<Booking> organizerBookings = gigIds.isEmpty() 
            ? List.of() 
            : bookingRepository.findByGigIds(gigIds);
        
        // Combine and deduplicate
        List<Booking> allBookings = new java.util.ArrayList<>(musicianBookings);
        organizerBookings.forEach(booking -> {
            if (!allBookings.contains(booking)) {
                allBookings.add(booking);
            }
        });
        
        return allBookings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto addPostGigMedia(UUID bookingId, UUID musicianId, List<String> mediaUrls) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getMusicianId().equals(musicianId)) {
            throw new IllegalArgumentException("Only the assigned musician can add post-gig media");
        }

        if (booking.getPostGigMediaUrls() == null) {
            booking.setPostGigMediaUrls(new java.util.ArrayList<>());
        }
        booking.getPostGigMediaUrls().addAll(mediaUrls);

        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    private BookingDto toDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .gigId(booking.getGigId())
                .musicianId(booking.getMusicianId())
                .organizerMandateId(booking.getOrganizerMandateId())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .acceptedAmount(booking.getAcceptedAmount())
                .acceptedAt(booking.getAcceptedAt())
                .musicianDoneAt(booking.getMusicianDoneAt())
                .ownerConfirmedAt(booking.getOwnerConfirmedAt())
                .completedAt(booking.getCompletedAt())
                .postGigMediaUrls(booking.getPostGigMediaUrls())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
