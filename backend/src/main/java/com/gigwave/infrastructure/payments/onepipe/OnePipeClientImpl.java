package com.gigwave.infrastructure.payments.onepipe;

import com.gigwave.infrastructure.payments.onepipe.OnePipeClient;
import com.gigwave.infrastructure.payments.onepipe.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * OnePipe API v2 Client Implementation.
 * Base URL: https://api.onepipe.io/v2/transact
 * Authentication: Authorization Bearer {api_key}, Signature MD5(request_ref;client_secret), Content-Type application/json.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnePipeClientImpl implements OnePipeClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /** OnePipe mock_mode: "Inspect" or "Live". Default Inspect for testing; set onepipe.mock-mode=Live for production. */
    @Value("${onepipe.mock-mode:Inspect}")
    private String mockMode;

    /** Encryption per OnePipe docs: CBC + MD5(secret UTF-16LE) key + UTF-16LE plaintext. Set onepipe.encryption=ecb to use ECB instead. */
    @Value("${onepipe.encryption:paywithaccount}")
    private String encryptionMode;

    /** PaywithAccount consent document URL for create mandate meta.customer_consent (per OnePipe/PaywithAccount). */
    private static final String CUSTOMER_CONSENT_URL = "https://paywithaccount.com/consent_template.pdf";

    @Override
    public MandateResponse setupMandate(MandateRequest request) {
        log.info("Setting up mandate for account: {}", request.getAccountNumber());

        // Same request_ref must appear in body and in Signature header (MD5(request_ref;secret_key))
        String requestRef = "REQ_" + System.currentTimeMillis();
        String transactionRef = "TXN_" + System.currentTimeMillis();

        String[] nameParts = parseName(request.getAccountName());
        String firstname = nameParts[0];
        String surname = nameParts.length > 1 ? nameParts[1] : "";

        long maxAmountKobo = request.getMaxAmount().multiply(new java.math.BigDecimal("100")).longValue();
        // secure = TripleDES.encrypt(accountNumber;bankCode) — trim to avoid env/DB spaces breaking decryption
        String accountNumber = request.getAccountNumber() != null ? request.getAccountNumber().trim() : "";
        String bankCode = request.getBankCode() != null ? request.getBankCode().trim() : "";
        String securePlain = accountNumber + ";" + bankCode;
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
        transaction.put("mock_mode", normalizeMockMode(mockMode));
        transaction.put("transaction_ref", transactionRef);
        transaction.put("transaction_desc", "Creating a mandate");
        transaction.put("transaction_ref_parent", null);
        transaction.put("amount", 0);
        Map<String, Object> customer = new HashMap<>();
        // customer_ref = Nigerian phone format: 234XXXXXXXXX (e.g. 08159089791 -> 2348159089791)
        String customerRef = normalizeNigerianPhone(request.getPhone());
        customer.put("customer_ref", customerRef != null ? customerRef : "");
        customer.put("firstname", firstname);
        customer.put("surname", surname);
        customer.put("email", request.getEmail() != null ? request.getEmail() : "");
        customer.put("mobile_no", customerRef != null ? customerRef : "");
        transaction.put("customer", customer);
        Map<String, Object> meta = new HashMap<>();
        meta.put("amount", String.valueOf(maxAmountKobo));
        meta.put("skip_consent", "true");
        // BVN always present: encrypted with same secret as auth.secure when provided, else ""
        if (request.getBvn() != null && !request.getBvn().isBlank()) {
            meta.put("bvn", encryptSecure(request.getBvn().trim()));
        } else {
            meta.put("bvn", "");
        }
        meta.put("biller_code", (billerCode != null) ? billerCode.trim() : "");
        meta.put("customer_consent", CUSTOMER_CONSENT_URL);
        meta.put("repeat_end_date", "2030-04-10-08-00-00");
        meta.put("repeat_frequency", "once");
        transaction.put("meta", meta);
        transaction.put("details", new HashMap<String, Object>());
        payload.put("transaction", transaction);

        // Signature uses same request_ref as payload.request_ref: MD5(request_ref;client_secret)
        HttpHeaders headers = createHeaders(requestRef);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        logRequest("create mandate", requestRef, payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            log.info("OnePipe create mandate response status: {}", response.getStatusCode());
            if (body != null) {
                // OnePipe can return status "Failed" with errors in body.errors or body.data.errors
                if ("Failed".equals(body.get("status"))) {
                    Object errors = body.get("errors");
                    if (errors == null && body.get("data") instanceof Map) {
                        errors = ((Map<?, ?>) body.get("data")).get("errors");
                    }
                    String errorMessage = errors != null ? extractErrorMessage(errors) : "Error occurred while processing request";
                    log.error("OnePipe create mandate error: {}", errorMessage);
                    throw new RuntimeException("OnePipe mandate setup failed: " + errorMessage);
                }
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
        } catch (HttpClientErrorException e) {
            log.error("OnePipe create mandate HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OnePipe API error: " + e.getResponseBodyAsString(), e);
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
            // Trim secret: env vars (Railway/Render/.env) often add trailing newline, breaking Signature and decryption
            String secret = secretKey != null ? secretKey.trim() : "";
            String result = "ecb".equalsIgnoreCase(encryptionMode != null ? encryptionMode.trim() : "")
                    ? OnePipeTripleDesUtil.encryptEcb(plaintext, secret)
                    : OnePipeTripleDesUtil.encrypt(plaintext, secret);
            if (log.isDebugEnabled()) {
                log.debug("OnePipe TripleDES: mode={}, plaintext length={}, result Base64 length={}", encryptionMode, plaintext != null ? plaintext.length() : 0, result != null ? result.length() : 0);
            }
            return result;
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

    /** OnePipe doc uses "Inspect" and "Live" (capital first letter). Normalize config value to match. */
    private String normalizeMockMode(String value) {
        if (value == null || value.isBlank()) return "Live";
        String v = value.trim();
        if ("inspect".equalsIgnoreCase(v)) return "Inspect";
        if ("live".equalsIgnoreCase(v)) return "Live";
        return v;
    }

    /** Normalize to Nigerian format 234XXXXXXXXX (no +, no leading 0). PaywithAccount/NIBSS often expect this. */
    private String normalizeNigerianPhone(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String s = phone.trim().replaceAll("\\s+", "");
        if (s.startsWith("+")) s = s.substring(1);
        if (s.startsWith("234")) return s;
        if (s.startsWith("0")) return "234" + s.substring(1);
        if (s.length() == 10 && s.matches("\\d{10}")) return "234" + s;
        return s;
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

        // secure = TripleDES.encrypt(accountNumber;bankCode) — trim to avoid spaces breaking decryption
        String accountNumber = request.getAccountNumber() != null ? request.getAccountNumber().trim() : "";
        String bankCode = request.getBankCode() != null ? request.getBankCode().trim() : "";
        String securePlain = accountNumber + ";" + bankCode;
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
        transaction.put("mock_mode", normalizeMockMode(mockMode));
        transaction.put("transaction_ref", transactionRef);
        transaction.put("transaction_desc", request.getNarration() != null && !request.getNarration().isBlank() ? request.getNarration().trim() : "A nice narration");
        transaction.put("transaction_ref_parent", null);
        transaction.put("amount", amountKobo);
        Map<String, Object> customer = new HashMap<>();
        // customer_ref = Nigerian phone format: 234XXXXXXXXX (e.g. 08159089791 -> 2348159089791)
        String customerRef = normalizeNigerianPhone(request.getPhone());
        customer.put("customer_ref", customerRef != null ? customerRef : "");
        customer.put("firstname", firstname);
        customer.put("surname", surname);
        customer.put("email", request.getEmail() != null ? request.getEmail() : "");
        customer.put("mobile_no", customerRef != null ? customerRef : "");
        transaction.put("customer", customer);
        Map<String, Object> meta = new HashMap<>();
        meta.put("biller_code", (billerCode != null) ? billerCode.trim() : "");
        meta.put("skip_consent", "true");
        meta.put("customer_consent", "");
        transaction.put("meta", meta);
        transaction.put("details", new HashMap<String, Object>());
        payload.put("transaction", transaction);

        HttpHeaders headers = createHeaders(requestRef);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        logRequest("collect", requestRef, payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            log.info("OnePipe collect response status: {}", response.getStatusCode());
            if (body != null) {
                if ("Failed".equals(body.get("status"))) {
                    Object errors = body.get("errors");
                    if (errors == null && body.get("data") instanceof Map) {
                        errors = ((Map<?, ?>) body.get("data")).get("errors");
                    }
                    String errorMessage = errors != null ? extractErrorMessage(errors) : "Error occurred while processing request";
                    log.error("OnePipe collect error: {}", errorMessage);
                    throw new RuntimeException("OnePipe collect failed: " + errorMessage);
                }
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
        } catch (HttpClientErrorException e) {
            log.error("OnePipe collect HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OnePipe API error: " + e.getResponseBodyAsString(), e);
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
        // Trim keys: env vars often have trailing newline, which breaks Signature and OnePipe auth
        String key = apiKey != null ? apiKey.trim() : "";
        String secret = secretKey != null ? secretKey.trim() : "";
        headers.set("Authorization", "Bearer " + key);

        if (requestRef != null && !requestRef.isBlank()) {
            try {
                String signature = md5Hex(requestRef + ";" + secret);
                headers.set("Signature", signature);
                if (log.isDebugEnabled()) {
                    log.debug("OnePipe Signature: request_ref={}, secret length={}, signature={}", requestRef, secret.length(), signature);
                }
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

    /** Log request for debugging (safe: no secret/signature value at INFO). */
    private void logRequest(String requestType, String requestRef, Map<String, Object> payload, HttpHeaders headers) {
        log.info("OnePipe request: type={}, request_ref={}, url={}, mock_mode={}", requestType, requestRef, baseUrl, mockMode);
        if (log.isDebugEnabled()) {
            try {
                log.debug("OnePipe payload: {}", OBJECT_MAPPER.writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                log.debug("OnePipe payload: (serialization failed)");
            }
            String sig = headers.getFirst("Signature");
            log.debug("OnePipe Signature: {}", sig != null ? sig : "(none)");
            @SuppressWarnings("unchecked")
            Map<String, Object> auth = (Map<String, Object>) payload.get("auth");
            if (auth != null) {
                String secure = (String) auth.get("secure");
                log.debug("OnePipe auth.secure length: {}", secure != null ? secure.length() : 0);
            }
        }
    }
}
