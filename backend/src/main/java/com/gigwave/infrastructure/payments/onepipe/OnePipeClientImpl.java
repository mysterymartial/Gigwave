package com.gigwave.infrastructure.payments.onepipe;

import com.gigwave.infrastructure.payments.onepipe.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnePipeClientImpl implements OnePipeClient {
    private final RestTemplate restTemplate;

    @Value("${onepipe.base-url}")
    private String baseUrl;

    @Value("${onepipe.api-key}")
    private String apiKey;

    @Value("${onepipe.secret-key}")
    private String secretKey;

    @Value("${onepipe.environment}")
    private String environment;

    @Override
    public MandateResponse setupMandate(MandateRequest request) {
        log.info("Setting up mandate for account: {}", request.getAccountNumber());
        
        // OnePipe API structure following OnePipe PWA documentation
        String requestRef = "REQ_" + System.currentTimeMillis();
        String transactionRef = "TXN_" + System.currentTimeMillis();
        
        // Parse account name to extract firstname and surname
        String[] nameParts = parseName(request.getAccountName());
        String firstname = nameParts[0];
        String surname = nameParts.length > 1 ? nameParts[1] : "";
        
        // Convert max amount to kobo (1 Naira = 100 kobo)
        long maxAmountKobo = request.getMaxAmount().multiply(new java.math.BigDecimal("100")).longValue();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("request_ref", requestRef);
        payload.put("request_type", "setup_mandate");
        
        // Auth object for mandate setup
        Map<String, Object> auth = new HashMap<>();
        auth.put("type", "bank.account");
        auth.put("secure", request.getAccountNumber()); // Account number for mandate setup
        auth.put("auth_provider", environment); // Use environment as provider
        payload.put("auth", auth);
        
        // Transaction object
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("mock_mode", "live");
        transaction.put("transaction_ref", transactionRef);
        transaction.put("transaction_desc", "Direct debit mandate setup");
        transaction.put("amount", maxAmountKobo);
        
        // Customer object
        Map<String, Object> customer = new HashMap<>();
        customer.put("customer_ref", request.getUserId() != null ? request.getUserId().toString() : "");
        customer.put("firstname", firstname);
        customer.put("surname", surname);
        customer.put("email", request.getEmail() != null ? request.getEmail() : "");
        customer.put("mobile_no", request.getPhone() != null ? request.getPhone() : "");
        transaction.put("customer", customer);
        
        // Meta object (optional)
        Map<String, Object> meta = new HashMap<>();
        meta.put("callback_url", request.getCallbackUrl());
        transaction.put("meta", meta);
        
        // Details object for mandate setup
        Map<String, Object> details = new HashMap<>();
        details.put("destination_account", request.getAccountNumber());
        details.put("destination_bank_code", request.getBankCode());
        details.put("max_amount", maxAmountKobo);
        transaction.put("details", details);
        
        payload.put("transaction", transaction);

        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                // Check for errors
                Object errors = body.get("errors");
                if (errors != null) {
                    String errorMessage = "OnePipe API error";
                    if (errors instanceof java.util.List && !((java.util.List<?>) errors).isEmpty()) {
                        Object firstError = ((java.util.List<?>) errors).get(0);
                        if (firstError instanceof Map) {
                            Object msg = ((Map<?, ?>) firstError).get("message");
                            errorMessage = msg != null ? msg.toString() : errorMessage;
                        }
                    }
                    log.error("OnePipe mandate setup error: {}", errorMessage);
                    throw new RuntimeException("OnePipe mandate setup failed: " + errorMessage);
                }
                
                Map<String, Object> transactionResponse = (Map<String, Object>) body.getOrDefault("transaction", new HashMap<>());
                return MandateResponse.builder()
                        .status((String) body.getOrDefault("status", "pending"))
                        .mandateRef((String) transactionResponse.getOrDefault("mandate_ref", ""))
                        .authorizationUrl((String) transactionResponse.getOrDefault("authorization_url", ""))
                        .message((String) body.getOrDefault("message", ""))
                        .build();
            }
        } catch (RuntimeException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Error setting up mandate", e);
            throw new RuntimeException("Failed to setup mandate: " + e.getMessage(), e);
        }

        // Should not reach here if real API is configured
        throw new RuntimeException("OnePipe API not properly configured or unavailable");
    }
    
    private String[] parseName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 1 ? new String[]{parts[0], ""} : parts;
    }

    @Override
    public DebitResponse initiateDebit(DebitRequest request) {
        log.info("Initiating collect (debit) for mandate: {}", request.getMandateRef());

        // OnePipe API structure following OnePipe PWA documentation
        String requestRef = "REQ_" + System.currentTimeMillis();
        String transactionRef = "TXN_" + System.currentTimeMillis();
        
        // Parse account name to extract firstname and surname
        String[] nameParts = parseName(request.getAccountName());
        String firstname = nameParts[0];
        String surname = nameParts.length > 1 ? nameParts[1] : "";
        
        // Convert amount to kobo (1 Naira = 100 kobo)
        long amountKobo = request.getAmount().multiply(new java.math.BigDecimal("100")).longValue();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("request_ref", requestRef);
        payload.put("request_type", "collect");
        
        // Auth object for collect (using mandate reference)
        Map<String, Object> auth = new HashMap<>();
        auth.put("type", "bank.account");
        auth.put("secure", request.getMandateRef()); // Mandate reference for collect
        auth.put("auth_provider", environment);
        payload.put("auth", auth);
        
        // Transaction object
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("mock_mode", "live");
        transaction.put("transaction_ref", transactionRef);
        transaction.put("transaction_desc", request.getNarration());
        transaction.put("amount", amountKobo);
        
        // Customer object
        Map<String, Object> customer = new HashMap<>();
        customer.put("customer_ref", request.getUserId() != null ? request.getUserId().toString() : "");
        customer.put("firstname", firstname);
        customer.put("surname", surname);
        customer.put("email", request.getEmail() != null ? request.getEmail() : "");
        customer.put("mobile_no", request.getPhone() != null ? request.getPhone() : "");
        transaction.put("customer", customer);
        
        // Meta object (optional)
        Map<String, Object> meta = new HashMap<>();
        meta.put("callback_url", request.getCallbackUrl());
        transaction.put("meta", meta);
        
        // Details set to null for collect requests
        transaction.put("details", null);
        
        payload.put("transaction", transaction);

        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String status = (String) body.getOrDefault("status", "pending");
                Map<String, Object> transactionResponse = (Map<String, Object>) body.getOrDefault("transaction", new HashMap<>());
                
                DebitResponse.DebitResponseBuilder responseBuilder = DebitResponse.builder()
                        .status(status)
                        .transactionRef((String) transactionResponse.getOrDefault("transaction_ref", transactionRef))
                        .message((String) body.getOrDefault("message", ""));
                
                // Handle WaitingForOTP status
                if ("WaitingForOTP".equalsIgnoreCase(status) || "waiting_for_otp".equalsIgnoreCase(status)) {
                    responseBuilder.otpReference((String) transactionResponse.getOrDefault("otp_reference", transactionRef));
                    responseBuilder.validationUrl(baseUrl + "/validate");
                }
                
                // Check for errors
                Object errors = body.get("errors");
                if (errors != null) {
                    String errorMessage = "OnePipe API error";
                    if (errors instanceof java.util.List && !((java.util.List<?>) errors).isEmpty()) {
                        Object firstError = ((java.util.List<?>) errors).get(0);
                        if (firstError instanceof Map) {
                            Object msg = ((Map<?, ?>) firstError).get("message");
                            errorMessage = msg != null ? msg.toString() : errorMessage;
                        }
                    }
                    log.error("OnePipe collect error: {}", errorMessage);
                    throw new RuntimeException("OnePipe collect failed: " + errorMessage);
                }
                
                return responseBuilder.build();
            }
        } catch (RuntimeException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Error initiating collect (debit)", e);
            throw new RuntimeException("Failed to initiate collect: " + e.getMessage(), e);
        }

        // Should not reach here if real API is configured
        throw new RuntimeException("OnePipe API not properly configured or unavailable");
    }

    // NOTE: initiatePayout (disburse) has been removed from OnePipe
    // Transfers are now handled by TransferClient (Flutterwave/Paystack/etc.)

    @Override
    public BankListResponse getSupportedBanks() {
        log.info("Fetching supported banks");

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // OnePipe banks endpoint is typically at /v2/banks or /banks
            // Try the standard endpoint first
            String banksUrl = baseUrl.replace("/transact", "/banks");
            if (banksUrl.equals(baseUrl)) {
                // If replacement didn't work, try alternative
                banksUrl = "https://api.onepipe.io/v2/banks";
            }
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    banksUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                // Parse OnePipe banks response
                Object banksData = body.get("data");
                if (banksData instanceof java.util.List) {
                    java.util.List<Map<String, Object>> banksList = (java.util.List<Map<String, Object>>) banksData;
                    java.util.List<BankListResponse.Bank> banks = banksList.stream()
                            .map(bank -> BankListResponse.Bank.builder()
                                    .code((String) bank.getOrDefault("code", ""))
                                    .name((String) bank.getOrDefault("name", ""))
                                    .build())
                            .collect(java.util.stream.Collectors.toList());
                    return BankListResponse.builder().banks(banks).build();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching banks from OnePipe, using fallback list", e);
        }

        // Fallback: Return common Nigerian banks if API call fails
        return BankListResponse.builder()
                .banks(java.util.Arrays.asList(
                        BankListResponse.Bank.builder().code("058").name("Guaranty Trust Bank").build(),
                        BankListResponse.Bank.builder().code("011").name("First Bank of Nigeria").build(),
                        BankListResponse.Bank.builder().code("014").name("Access Bank").build(),
                        BankListResponse.Bank.builder().code("232").name("Sterling Bank").build(),
                        BankListResponse.Bank.builder().code("033").name("United Bank for Africa").build(),
                        BankListResponse.Bank.builder().code("050").name("Ecobank Nigeria").build(),
                        BankListResponse.Bank.builder().code("070").name("Fidelity Bank").build(),
                        BankListResponse.Bank.builder().code("057").name("Zenith Bank").build()
                ))
                .build();
    }
    
    @Override
    public DebitResponse validateOtp(String transactionRef, String otp) {
        log.info("Validating OTP for transaction: {}", transactionRef);
        
        String requestRef = "REQ_" + System.currentTimeMillis();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("request_ref", requestRef);
        payload.put("request_type", "validate");
        
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transaction_ref", transactionRef);
        transaction.put("otp", otp);
        payload.put("transaction", transaction);
        
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/validate",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            
            Map<String, Object> body = response.getBody();
            if (body != null) {
                Map<String, Object> transactionResponse = (Map<String, Object>) body.getOrDefault("transaction", new HashMap<>());
                return DebitResponse.builder()
                        .status((String) body.getOrDefault("status", "pending"))
                        .transactionRef((String) transactionResponse.getOrDefault("transaction_ref", transactionRef))
                        .message((String) body.getOrDefault("message", ""))
                        .build();
            }
        } catch (Exception e) {
            log.error("Error validating OTP", e);
            throw new RuntimeException("Failed to validate OTP: " + e.getMessage(), e);
        }
        
        throw new RuntimeException("Invalid OTP validation response");
    }
    
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // OnePipe webhook signature verification
            // Typically uses HMAC-SHA256 with secret key
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            
            // Use constant-time comparison to prevent timing attacks
            return java.security.MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // OnePipe API uses Authorization Bearer and Signature headers
        headers.set("Authorization", "Bearer " + apiKey);
        
        // Generate signature (OnePipe requires HMAC signature)
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        
        try {
            String signature = generateSignature(timestamp);
            headers.set("Signature", signature);
        } catch (Exception e) {
            log.error("Error generating signature", e);
        }

        return headers;
    }

    private String generateSignature(String timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        String message = apiKey + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
