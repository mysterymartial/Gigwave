package com.gigwave.application.disputes;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.domain.bookings.Booking;
import com.gigwave.domain.disputes.Dispute;
import com.gigwave.domain.disputes.DisputeStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.infrastructure.persistence.disputes.DisputeRepository;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.application.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeService {
    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Dispute openDispute(UUID bookingId, UUID raisedBy, String reason, String evidenceUrl) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getMusicianId().equals(raisedBy) && !booking.getGigId().equals(raisedBy)) {
            throw new IllegalArgumentException("Only participants can raise disputes");
        }

        Dispute dispute = Dispute.builder()
                .bookingId(bookingId)
                .raisedBy(raisedBy)
                .reason(reason)
                .evidenceUrl(evidenceUrl)
                .status(DisputeStatus.OPEN)
                .build();

        dispute = disputeRepository.save(dispute);
        
        notificationService.sendDisputeRaisedNotification(bookingId);
        
        return dispute;
    }

    @Transactional
    public Dispute resolveDispute(UUID disputeId, DisputeStatus resolution, UUID resolvedBy) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));

        dispute.setStatus(resolution);
        dispute.setResolvedAt(LocalDateTime.now());

        if (resolution == DisputeStatus.RESOLVED_MUSICIAN || resolution == DisputeStatus.RESOLVED_ORGANIZER) {
            flagUserAsRisky(dispute.getRaisedBy());
        }

        return disputeRepository.save(dispute);
    }

    @Transactional
    public void flagUserAsRisky(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // In a real implementation, you might add a 'riskFlag' field or similar
        // For now, this is a placeholder for the business logic
    }

    public List<Dispute> listDisputesForBooking(UUID bookingId) {
        return disputeRepository.findByBookingId(bookingId);
    }

    public List<Dispute> listDisputesForUser(UUID userId) {
        return disputeRepository.findByRaisedBy(userId);
    }

    public Dispute getDispute(UUID disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
    }
}

