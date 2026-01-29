package com.gigwave.infrastructure.payments.onepipe;

import com.gigwave.infrastructure.payments.onepipe.OnePipeClient;
import com.gigwave.infrastructure.payments.onepipe.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    @Value("${onepipe.biller-code:}")
    private String billerCode;

    /** Default customer consent URL for create mandate (PaywithAccount consent template). */
    private static final String DEFAULT_CUSTOMER_CONSENT_URL = "https://paywithaccount.com/consent_template.pdf";

    @Override
    public MandateResponse setupMandate(MandateRequest request) {
        log.info("Setting up mandate for account: {}", request.getAccountNumber());

        String requestRef = "REQ_" + System.currentTimeMillis();
        String transactionRef = "TXN_" + System.currentTimeMillis();

        String[] nameParts = parseName(request.getAccountName());
        String firstname = nameParts[0];
        String surname = nameParts.length > 1 ? nameParts[1] : "";

        long maxAmountKobo = request.getMaxAmount().multiply(new java.math.BigDecimal("100")).longValue();
        String securePlain = request.getAccountNumber() + ";" + request.getBankCode();
        String secureEncrypted = encryptSecure(securePlain);

        Map<String, Object> payload = new HashMap<>();
        payload.put("request_ref", requestRef);
        payload.put("request_type", "create mandate");

        Map<String, Object> auth = new HashMap<>();
        auth.put("type", "bank.account");
        auth.put("secure", secureEncrypted);
        auth.put("auth_provider", "PaywithAccount");
        payload.put("auth", auth);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("mock_mode", "Live");
        transaction.put("transaction_ref", transactionRef);
        transaction.put("transaction_desc", "Creating a mandate");
        transaction.put("transaction_ref_parent", null);
        transaction.put("amount", 0);

        Map<String, Object> customer = new HashMap<>();
        customer.put("customer_ref", request.getUserId() != null ? request.getUserId().toString() : "");
        customer.put("firstname", firstname);
        customer.put("surname", surname);
        customer.put("email", request.getEmail() != null ? request.getEmail() : "");
        customer.put("mobile_no", request.getPhone() != null ? request.getPhone() : "");
        transaction.put("customer", customer);

        Map<String, Object> meta = new HashMap<>();
        meta.put("amount", String.valueOf(maxAmountKobo));
        meta.put("skip_consent", "true");
        if (request.getBvn() != null && !request.getBvn().isBlank()) {
            meta.put("bvn", encryptSecure(request.getBvn()));
        }
        if (billerCode != null && !billerCode.isBlank()) {
            meta.put("biller_code", billerCode);
        }
        String consentUrl = (request.getCallbackUrl() != null && !request.getCallbackUrl().isBlank())
                ? request.getCallbackUrl()
                : DEFAULT_CUSTOMER_CONSENT_URL;
        meta.put("customer_consent", consentUrl);
        meta.put("activation_method", "transfer");
        transaction.put("meta", meta);

        transaction.put("details", new HashMap<String, Object>());
        payload.put("transaction", transaction);

        HttpHeaders headers = createHeaders(requestRef);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                Object errors = body.get("errors");
                if (errors != null) {
                    String errorMessage = extractErrorMessage(errors);
                    log.error("OnePipe create mandate error: {}", errorMessage);
                    throw new RuntimeException("OnePipe mandate setup failed: " + errorMessage);
                }
                Map<String, Object> tx = (Map<String, Object>) body.getOrDefault("transaction", new HashMap<>());
                return MandateResponse.builder()
                        .status((String) body.getOrDefault("status", "pending"))
                        .mandateRef((String) tx.getOrDefault("mandate_ref", ""))
                        .authorizationUrl((String) tx.getOrDefault("authorization_url", ""))
                        .message((String) body.getOrDefault("message", ""))
                        .build();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error setting up mandate", e);
            throw new RuntimeException("Failed to setup mandate: " + e.getMessage(), e);
        }
        throw new RuntimeException("OnePipe API not properly configured or unavailable");
    }

    private String encryptSecure(String plaintext) {
        try {
            return OnePipeTripleDesUtil.encrypt(plaintext, secretKey);
        } catch (GeneralSecurityException e) {
            log.error("TripleDES encryption failed", e);
            throw new RuntimeException("OnePipe encryption failed: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessage(Object errors) {
        if (!(errors instanceof java.util.List) || ((java.util.List<?>) errors).isEmpty()) {
            return "OnePipe API error";
        }
        Object first = ((java.util.List<?>) errors).get(0);
        if (first instanceof Map) {
            Object msg = ((Map<?, ?>) first).get("message");
            return msg != null ? msg.toString() : "OnePipe API error";
        }
        return "OnePipe API error";
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

        if (request.getAccountNumber() == null || request.getBankCode() == null) {
            throw new IllegalArgumentException("Collect requires accountNumber and bankCode for OnePipe auth.secure encryption");
        }

        String requestRef = "REQ_" + System.currentTimeMillis();
        String transactionRef = "TXN_" + System.currentTimeMillis();

        String[] nameParts = parseName(request.getAccountName());
        String firstname = nameParts[0];
        String surname = nameParts.length > 1 ? nameParts[1] : "";

        long amountKobo = request.getAmount().multiply(new java.math.BigDecimal("100")).longValue();

        String securePlain = request.getAccountNumber() + ";" + request.getBankCode();
        String secureEncrypted = encryptSecure(securePlain);

        Map<String, Object> payload = new HashMap<>();
        payload.put("request_ref", requestRef);
        payload.put("request_type", "collect");

        Map<String, Object> auth = new HashMap<>();
        auth.put("type", "bank.account");
        auth.put("secure", secureEncrypted);
        auth.put("auth_provider", "NIBSS");
        payload.put("auth", auth);

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("mock_mode", "Live");
        transaction.put("transaction_ref", transactionRef);
        transaction.put("transaction_desc", request.getNarration());
        transaction.put("transaction_ref_parent", null);
        transaction.put("amount", amountKobo);

        Map<String, Object> customer = new HashMap<>();
        customer.put("customer_ref", request.getUserId() != null ? request.getUserId().toString() : "");
        customer.put("firstname", firstname);
        customer.put("surname", surname);
        customer.put("email", request.getEmail() != null ? request.getEmail() : "");
        customer.put("mobile_no", request.getPhone() != null ? request.getPhone() : "");
        transaction.put("customer", customer);

        Map<String, Object> meta = new HashMap<>();
        if (billerCode != null && !billerCode.isBlank()) {
            meta.put("biller_code", billerCode);
        }
        transaction.put("meta", meta);

        transaction.put("details", new HashMap<String, Object>());
        payload.put("transaction", transaction);

        HttpHeaders headers = createHeaders(requestRef);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                Object errors = body.get("errors");
                if (errors != null) {
                    String errorMessage = extractErrorMessage(errors);
                    log.error("OnePipe collect error: {}", errorMessage);
                    throw new RuntimeException("OnePipe collect failed: " + errorMessage);
                }
                String status = (String) body.getOrDefault("status", "pending");
                Map<String, Object> tx = (Map<String, Object>) body.getOrDefault("transaction", new HashMap<>());
                DebitResponse.DebitResponseBuilder rb = DebitResponse.builder()
                        .status(status)
                        .transactionRef((String) tx.getOrDefault("transaction_ref", transactionRef))
                        .message((String) body.getOrDefault("message", ""));
                if ("WaitingForOTP".equalsIgnoreCase(status) || "waiting_for_otp".equalsIgnoreCase(status)) {
                    rb.otpReference((String) tx.getOrDefault("otp_reference", transactionRef));
                    rb.validationUrl(baseUrl + "/validate");
                }
                return rb.build();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error initiating collect (debit)", e);
            throw new RuntimeException("Failed to initiate collect: " + e.getMessage(), e);
        }
        throw new RuntimeException("OnePipe API not properly configured or unavailable");
    }

    // NOTE: initiatePayout (disburse) has been removed from OnePipe
    // Transfers are now handled by TransferClient (Flutterwave/Paystack/etc.)

    @Override
    public BankListResponse getSupportedBanks() {
        log.info("Fetching supported banks");

        HttpHeaders headers = createHeaders(null);
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
                        BankListResponse.Bank.builder().code("044").name("Access Bank").build(),
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
        
        HttpHeaders headers = createHeaders(requestRef);
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
            // OnePipe webhook verification: MD5(secret + payload) as hex, per docs
            String computed = md5Hex(secretKey + payload);
            String received = (signature != null ? signature.trim() : "").toLowerCase();
            // Constant-time comparison to prevent timing attacks
            return !received.isEmpty()
                    && java.security.MessageDigest.isEqual(
                            computed.getBytes(StandardCharsets.UTF_8),
                            received.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private HttpHeaders createHeaders(String requestRef) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // Signature: MD5(request_ref;client_secret) per OnePipe docs
        if (requestRef != null && !requestRef.isBlank()) {
            try {
                String signature = md5Hex(requestRef + ";" + secretKey);
                headers.set("Signature", signature);
            } catch (Exception e) {
                log.error("Error generating signature", e);
            }
        }

        return headers;
    }

    /** MD5 hash of message as 32-char lowercase hex. Used for request Signature header and webhook verification. */
    private String md5Hex(String message) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b & 0xff));
        }
        return hex.toString();
    }
}
