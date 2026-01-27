package com.gigwave.infrastructure.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermiiClientImpl implements TermiiClient {
    private final RestTemplate restTemplate;

    @Value("${termii.api-key:}")
    private String apiKey;

    @Value("${termii.base-url:https://v3.api.termii.com}")
    private String baseUrl;

    @Value("${termii.sender-id:GigWave}")
    private String defaultSenderId;

    @Value("${termii.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${termii.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${termii.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Override
    public boolean sendSms(String phone, String message, String senderId) {
        if (!smsEnabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("SMS not enabled or API key not configured. Message would be: To: {}, Message: {}", phone, message);
            return false;
        }

        try {
            // Format phone number (ensure it starts with country code)
            String formattedPhone = formatPhoneNumber(phone);

            // Termii SMS API endpoint (v3 API)
            String url = baseUrl + "/sms/send";

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", formattedPhone);
            payload.put("from", senderId != null ? senderId : defaultSenderId);
            payload.put("sms", message);
            payload.put("type", "plain");
            payload.put("channel", "generic");
            payload.put("api_key", apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String code = (String) body.getOrDefault("code", "");
                if ("ok".equalsIgnoreCase(code) || "200".equals(code) || response.getStatusCode().is2xxSuccessful()) {
                    log.info("SMS sent successfully to: {}", formattedPhone);
                    return true;
                } else {
                    log.error("Failed to send SMS to {}: {}", formattedPhone, body.getOrDefault("message", "Unknown error"));
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", phone, e.getMessage(), e);
            return false;
        }

        return false;
    }

    @Override
    public boolean sendEmail(String email, String subject, String message) {
        if (!emailEnabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("Email not enabled or API key not configured. Email would be: To: {}, Subject: {}, Message: {}", email, subject, message);
            return false;
        }

        try {
            // Termii Email API endpoint (v3 API)
            String url = baseUrl + "/email/send";

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", email);
            payload.put("from", "noreply@gigwave.com");
            payload.put("subject", subject);
            payload.put("html_body", message);
            payload.put("text_body", message);
            payload.put("api_key", apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String code = (String) body.getOrDefault("code", "");
                if ("ok".equalsIgnoreCase(code) || "200".equals(code) || response.getStatusCode().is2xxSuccessful()) {
                    log.info("Email sent successfully to: {}", email);
                    return true;
                } else {
                    log.error("Failed to send email to {}: {}", email, body.getOrDefault("message", "Unknown error"));
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", email, e.getMessage(), e);
            return false;
        }

        return false;
    }

    @Override
    public boolean sendWhatsApp(String phone, String message) {
        if (!whatsappEnabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("WhatsApp not enabled or API key not configured. WhatsApp would be: To: {}, Message: {}", phone, message);
            return false;
        }

        try {
            // Format phone number
            String formattedPhone = formatPhoneNumber(phone);

            // Termii WhatsApp API endpoint (v3 API)
            String url = baseUrl + "/whatsapp/send";

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", formattedPhone);
            payload.put("from", defaultSenderId);
            payload.put("message", message);
            payload.put("api_key", apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String code = (String) body.getOrDefault("code", "");
                if ("ok".equalsIgnoreCase(code) || "200".equals(code) || response.getStatusCode().is2xxSuccessful()) {
                    log.info("WhatsApp message sent successfully to: {}", formattedPhone);
                    return true;
                } else {
                    log.error("Failed to send WhatsApp to {}: {}", formattedPhone, body.getOrDefault("message", "Unknown error"));
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error sending WhatsApp to {}: {}", phone, e.getMessage(), e);
            return false;
        }

        return false;
    }

    /**
     * Format phone number to include country code if missing
     * Nigerian numbers should start with 234
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        // Remove any spaces, dashes, or parentheses
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");

        // If it starts with 0, replace with 234 (Nigeria country code)
        if (cleaned.startsWith("0")) {
            cleaned = "234" + cleaned.substring(1);
        }
        // If it doesn't start with country code, add 234
        else if (!cleaned.startsWith("234") && !cleaned.startsWith("+234")) {
            cleaned = "234" + cleaned;
        }
        // Remove + if present
        else if (cleaned.startsWith("+234")) {
            cleaned = cleaned.substring(1);
        }

        return cleaned;
    }
}
