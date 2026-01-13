package com.gigwave.api.controllers;

import com.gigwave.application.payments.PaymentService;
import com.gigwave.infrastructure.payments.onepipe.dto.BankListResponse;
import com.gigwave.infrastructure.payments.onepipe.dto.MandateResponse;
import com.gigwave.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/banks")
    public ResponseEntity<BankListResponse> getSupportedBanks() {
        BankListResponse banks = paymentService.getSupportedBanks();
        return ResponseEntity.ok(banks);
    }

    @GetMapping("/platform-fee")
    public ResponseEntity<Map<String, Object>> getPlatformFee() {
        return ResponseEntity.ok(paymentService.getPlatformFeeInfo());
    }

    @PostMapping("/mandate/setup/organizer")
    public ResponseEntity<MandateResponse> setupOrganizerMandate(
            @RequestParam UUID bankAccountId,
            @RequestParam BigDecimal maxAmount,
            @CurrentUser UUID userId
    ) {
        MandateResponse response = paymentService.setupMandateForOrganizer(userId, bankAccountId, maxAmount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mandate/setup/musician")
    public ResponseEntity<MandateResponse> setupMusicianMandate(
            @RequestParam UUID bankAccountId,
            @RequestParam BigDecimal maxAmount,
            @CurrentUser UUID userId
    ) {
        MandateResponse response = paymentService.setupMandateForMusician(userId, bankAccountId, maxAmount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingId}/debit")
    public ResponseEntity<Map<String, String>> initiateDebit(
            @PathVariable UUID bookingId
    ) {
        var response = paymentService.initiateDebitForBooking(bookingId);
        return ResponseEntity.ok(Map.of(
                "status", response.getStatus(),
                "transactionRef", response.getTransactionRef(),
                "message", response.getMessage()
        ));
    }

    @PostMapping("/webhooks/mandate")
    public ResponseEntity<Void> handleMandateWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "X-OnePipe-Signature", required = false) String signature
    ) {
        // Verify webhook signature if provided
        if (signature != null && !paymentService.verifyWebhookSignature(requestBody, signature)) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        // Parse JSON payload
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);
            
            String mandateRef = (String) payload.get("mandate_ref");
            String status = (String) payload.get("status");
            paymentService.handleMandateWebhook(mandateRef, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).build(); // Bad Request
        }
    }

    @PostMapping("/webhooks/debit")
    public ResponseEntity<Void> handleDebitWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "X-OnePipe-Signature", required = false) String signature
    ) {
        // Verify webhook signature if provided
        if (signature != null && !paymentService.verifyWebhookSignature(requestBody, signature)) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        // Parse JSON payload
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);
            
            String transactionRef = (String) payload.get("transaction_ref");
            String status = (String) payload.get("status");
            paymentService.handleDebitWebhook(transactionRef, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).build(); // Bad Request
        }
    }

    @PostMapping("/webhooks/payout")
    public ResponseEntity<Void> handlePayoutWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "X-OnePipe-Signature", required = false) String signature
    ) {
        // Verify webhook signature if provided
        if (signature != null && !paymentService.verifyWebhookSignature(requestBody, signature)) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        // Parse JSON payload
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);
            
            String transactionRef = (String) payload.get("transaction_ref");
            String status = (String) payload.get("status");
            paymentService.handlePayoutWebhook(transactionRef, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).build(); // Bad Request
        }
    }
    
    @PostMapping("/bookings/{bookingId}/validate-otp")
    public ResponseEntity<Map<String, String>> validateOtp(
            @PathVariable UUID bookingId,
            @RequestParam String otp
    ) {
        var response = paymentService.validateOtpForBooking(bookingId, otp);
        return ResponseEntity.ok(Map.of(
                "status", response.getStatus(),
                "transactionRef", response.getTransactionRef(),
                "message", response.getMessage()
        ));
    }
}


