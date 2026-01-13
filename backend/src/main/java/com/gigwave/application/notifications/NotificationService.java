package com.gigwave.application.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    // In a real implementation, this would integrate with SMS/Email/WhatsApp providers

    public void sendBookingAcceptedNotification(UUID musicianId, UUID bookingId) {
        log.info("Sending booking accepted notification to musician: {}, booking: {}", musicianId, bookingId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }

    public void sendPaymentInitiatedNotification(UUID organizerId, UUID bookingId) {
        log.info("Sending payment initiated notification to organizer: {}, booking: {}", organizerId, bookingId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }

    public void sendPaymentSuccessNotification(UUID musicianId, UUID bookingId) {
        log.info("Sending payment success notification to musician: {}, booking: {}", musicianId, bookingId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }

    public void sendPaymentFailedNotification(UUID organizerId, UUID bookingId) {
        log.info("Sending payment failed notification to organizer: {}, booking: {}", organizerId, bookingId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }

    public void sendGigAcceptedNotification(UUID organizerId, UUID gigId) {
        log.info("Sending gig accepted notification to organizer: {}, gig: {}", organizerId, gigId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }

    public void sendDisputeRaisedNotification(UUID bookingId) {
        log.info("Sending dispute raised notification for booking: {}", bookingId);
        // TODO: Integrate with SMS/Email/WhatsApp provider
    }
}





