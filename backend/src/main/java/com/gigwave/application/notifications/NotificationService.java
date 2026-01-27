package com.gigwave.application.notifications;

import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.infrastructure.notifications.TermiiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final UserRepository userRepository;
    private final TermiiClient termiiClient;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${notification.provider.sms:log}")
    private String smsProvider;

    @Value("${notification.provider.email:log}")
    private String emailProvider;

    @Value("${notification.provider.whatsapp:log}")
    private String whatsappProvider;

    @Value("${termii.sender-id:GigWave}")
    private String senderId;

    /**
     * Send notification to a user via SMS, Email, or WhatsApp
     * Uses Termii for production notifications (SMS, Email, WhatsApp)
     */
    private void sendNotification(UUID userId, String message, String subject) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            log.warn("Cannot send notification: User {} not found", userId);
            return;
        }

        String phone = user.getPhone();
        String email = user.getEmail();

        // Log notification (for development/testing)
        log.info("=== NOTIFICATION ===");
        log.info("To: {} (Phone: {}, Email: {})", userId, phone, email);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("===================");

        // Send via configured channels
        if (smsEnabled && phone != null) {
            sendSms(phone, message);
        }

        if (emailEnabled && email != null) {
            sendEmail(email, subject, message);
        }

        if (whatsappEnabled && phone != null) {
            sendWhatsApp(phone, message);
        }
    }

    private void sendSms(String phone, String message) {
        if ("log".equals(smsProvider)) {
            log.info("[SMS] To: {}, Message: {}", phone, message);
        } else if ("termii".equalsIgnoreCase(smsProvider)) {
            boolean sent = termiiClient.sendSms(phone, message, senderId);
            if (sent) {
                log.info("[SMS] Successfully sent via Termii to: {}", phone);
            } else {
                log.error("[SMS] Failed to send via Termii to: {}", phone);
            }
        } else {
            log.warn("[SMS] Unknown provider: {}. Message would be: To: {}, Message: {}", smsProvider, phone, message);
        }
    }

    private void sendEmail(String email, String subject, String message) {
        if ("log".equals(emailProvider)) {
            log.info("[EMAIL] To: {}, Subject: {}, Message: {}", email, subject, message);
        } else if ("termii".equalsIgnoreCase(emailProvider)) {
            boolean sent = termiiClient.sendEmail(email, subject, message);
            if (sent) {
                log.info("[EMAIL] Successfully sent via Termii to: {}", email);
            } else {
                log.error("[EMAIL] Failed to send via Termii to: {}", email);
            }
        } else {
            log.warn("[EMAIL] Unknown provider: {}. Email would be: To: {}, Subject: {}, Message: {}", emailProvider, email, subject, message);
        }
    }

    private void sendWhatsApp(String phone, String message) {
        if ("log".equals(whatsappProvider)) {
            log.info("[WHATSAPP] To: {}, Message: {}", phone, message);
        } else if ("termii".equalsIgnoreCase(whatsappProvider)) {
            boolean sent = termiiClient.sendWhatsApp(phone, message);
            if (sent) {
                log.info("[WHATSAPP] Successfully sent via Termii to: {}", phone);
            } else {
                log.error("[WHATSAPP] Failed to send via Termii to: {}", phone);
            }
        } else {
            log.warn("[WHATSAPP] Unknown provider: {}. WhatsApp would be: To: {}, Message: {}", whatsappProvider, phone, message);
        }
    }

    public void sendBookingAcceptedNotification(UUID musicianId, UUID bookingId) {
        String message = String.format(
            "Congratulations! Your booking request has been accepted. Booking ID: %s. Check your dashboard for details.",
            bookingId
        );
        sendNotification(musicianId, message, "Booking Accepted");
    }

    public void sendPaymentInitiatedNotification(UUID organizerId, UUID bookingId) {
        String message = String.format(
            "Payment has been initiated for booking %s. Please complete the payment process.",
            bookingId
        );
        sendNotification(organizerId, message, "Payment Initiated");
    }

    public void sendPaymentSuccessNotification(UUID musicianId, UUID bookingId) {
        String message = String.format(
            "Payment successful! You have been paid for booking %s. Check your account for details.",
            bookingId
        );
        sendNotification(musicianId, message, "Payment Successful");
    }

    public void sendPaymentFailedNotification(UUID organizerId, UUID bookingId) {
        String message = String.format(
            "Payment failed for booking %s. Please try again or contact support.",
            bookingId
        );
        sendNotification(organizerId, message, "Payment Failed");
    }

    public void sendGigAcceptedNotification(UUID organizerId, UUID gigId) {
        String message = String.format(
            "A musician has been selected for your gig %s. Check your dashboard for details.",
            gigId
        );
        sendNotification(organizerId, message, "Musician Selected");
    }

    public void sendDisputeRaisedNotification(UUID bookingId) {
        String message = String.format(
            "A dispute has been raised for booking %s. Please review and take appropriate action.",
            bookingId
        );
        // Note: This would typically notify admins, but for now we log it
        log.info("Dispute raised for booking: {}", bookingId);
    }

    /**
     * Notify organizer when a musician bids on their gig
     */
    public void sendNewBidNotification(UUID organizerId, UUID bookingId, BigDecimal acceptedAmount) {
        String message = String.format(
            "New bid received! A musician has bid ₦%.2f for your gig. Booking ID: %s. Check your dashboard to review.",
            acceptedAmount, bookingId
        );
        sendNotification(organizerId, message, "New Bid Received");
    }

    /**
     * Notify musicians when a new gig is created
     */
    public void sendNewGigNotification(UUID musicianId, UUID gigId, String gigTitle, String location) {
        String message = String.format(
            "New gig available: %s at %s. Check it out and submit your bid! Gig ID: %s",
            gigTitle, location, gigId
        );
        sendNotification(musicianId, message, "New Gig Available");
    }
}
