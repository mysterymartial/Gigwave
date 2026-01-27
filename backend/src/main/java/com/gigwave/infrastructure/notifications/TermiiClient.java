package com.gigwave.infrastructure.notifications;

public interface TermiiClient {
    /**
     * Send SMS via Termii
     */
    boolean sendSms(String phone, String message, String senderId);
    
    /**
     * Send Email via Termii
     */
    boolean sendEmail(String email, String subject, String message);
    
    /**
     * Send WhatsApp message via Termii
     */
    boolean sendWhatsApp(String phone, String message);
}
