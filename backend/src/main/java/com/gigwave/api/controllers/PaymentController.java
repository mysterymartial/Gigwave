package com.gigwave.api.controllers;

import com.gigwave.application.payments.PaymentService;
import com.gigwave.domain.payments.IdempotencyRecord;
import com.gigwave.infrastructure.persistence.payments.IdempotencyRepository;
import com.gigwave.infrastructure.payments.onepipe.dto.BankListResponse;
import com.gigwave.infrastructure.payments.onepipe.dto.MandateResponse;
import com.gigwave.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private static final long IDEMPOTENCY_TTL_MS = 24 * 60 * 60 * 1000L;

    private final PaymentService paymentService;
    private final IdempotencyRepository idempotencyRepository;

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
            @CurrentUser UUID userId,
            @RequestParam(required = false) String bvn,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (userId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return mandateWithIdempotency(idempotencyKey,
                () -> paymentService.setupMandateForOrganizer(userId, bankAccountId, maxAmount, bvn));
    }

    @PostMapping("/mandate/setup/musician")
    public ResponseEntity<MandateResponse> setupMusicianMandate(
            @RequestParam UUID bankAccountId,
            @RequestParam BigDecimal maxAmount,
            @CurrentUser UUID userId,
            @RequestParam(required = false) String bvn,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (userId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return mandateWithIdempotency(idempotencyKey,
                () -> paymentService.setupMandateForMusician(userId, bankAccountId, maxAmount, bvn));
    }

    private ResponseEntity<MandateResponse> mandateWithIdempotency(String key, Supplier<MandateResponse> supplier) {
        if (key != null && !key.isBlank()) {
            var existing = idempotencyRepository.findByKey(key);
            if (existing.isPresent() && existing.get().getExpiresAt().after(new Date())) {
                var r = existing.get();
                return ResponseEntity.ok(MandateResponse.builder()
                        .mandateRef(r.getMandateRef()).authorizationUrl(r.getAuthorizationUrl())
                        .status(r.getStatus()).message(r.getMessage() != null ? r.getMessage() : "").build());
            }
        }
        MandateResponse resp = supplier.get();
        if (key != null && !key.isBlank()) {
            idempotencyRepository.save(IdempotencyRecord.builder()
                    .id(UUID.randomUUID()).key(key)
                    .mandateRef(resp.getMandateRef()).authorizationUrl(resp.getAuthorizationUrl())
                    .status(resp.getStatus()).message(resp.getMessage())
                    .expiresAt(new Date(System.currentTimeMillis() + IDEMPOTENCY_TTL_MS)).build());
        }
        return ResponseEntity.ok(resp);
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

    /**
     * Single OnePipe webhook URL for both mandate and debit notifications.
     * Configure this URL in the OnePipe dashboard; mandate and debit callbacks use it.
     */
    @PostMapping("/webhooks/onepipe")
    public ResponseEntity<Void> handleOnePipeWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "X-OnePipe-Signature") String signature
    ) {
        if (signature == null || signature.isBlank() || !paymentService.verifyWebhookSignature(requestBody, signature)) {
            return ResponseEntity.status(401).build();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);
            String status = (String) payload.get("status");

            if (payload.containsKey("mandate_ref")) {
                String mandateRef = (String) payload.get("mandate_ref");
                paymentService.handleMandateWebhook(mandateRef, status);
                return ResponseEntity.ok().build();
            }
            if (payload.containsKey("transaction_ref")) {
                String transactionRef = (String) payload.get("transaction_ref");
                paymentService.handleDebitWebhook(transactionRef, status);
                return ResponseEntity.ok().build();
            }
            log.warn("OnePipe webhook: unknown payload shape");
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            log.error("OnePipe webhook error", e);
            return ResponseEntity.status(400).build();
        }
    }

    @PostMapping("/webhooks/payout")
    public ResponseEntity<Void> handlePayoutWebhook(
            @RequestBody String requestBody,
            @RequestHeader(value = "X-OnePipe-Signature", required = false) String onePipeSignature,
            @RequestHeader(value = "verif-hash", required = false) String flutterwaveHash
    ) {
        boolean onePipeValid = onePipeSignature != null && !onePipeSignature.isBlank()
                && paymentService.verifyWebhookSignature(requestBody, onePipeSignature);
        boolean flwValid = flutterwaveHash != null && !flutterwaveHash.isBlank();
        if (!onePipeValid && !flwValid) {
            return ResponseEntity.status(401).build(); // Require at least one valid signature
        }
        if (onePipeSignature != null && !onePipeSignature.isBlank() && !onePipeValid) {
            return ResponseEntity.status(401).build(); // OnePipe signature invalid
        }

        // Parse JSON payload
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(requestBody, Map.class);
            
            // Support both OnePipe and Flutterwave webhook formats
            String transactionRef = null;
            String status = null;
            
            // OnePipe format: { "transaction_ref": "...", "status": "..." }
            if (payload.containsKey("transaction_ref")) {
                transactionRef = (String) payload.get("transaction_ref");
                status = (String) payload.get("status");
            }
            // Flutterwave format: { "data": { "reference": "...", "status": "..." } }
            else if (payload.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                transactionRef = (String) data.get("reference");
                status = (String) data.get("status");
            }
            
            if (transactionRef != null && status != null) {
                paymentService.handlePayoutWebhook(transactionRef, status);
                return ResponseEntity.ok().build();
            } else {
                log.warn("Invalid webhook payload format: {}", requestBody);
                return ResponseEntity.status(400).build(); // Bad Request
            }
        } catch (Exception e) {
            log.error("Error processing payout webhook", e);
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
