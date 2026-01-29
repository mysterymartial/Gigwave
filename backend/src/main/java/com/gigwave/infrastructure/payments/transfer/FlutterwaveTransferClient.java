package com.gigwave.infrastructure.payments.transfer;

import com.gigwave.infrastructure.payments.transfer.dto.TransferRequest;
import com.gigwave.infrastructure.payments.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlutterwaveTransferClient implements TransferClient {
    private final RestTemplate restTemplate;

    @Value("${flutterwave.base-url:https://api.flutterwave.com/v3}")
    private String baseUrl;

    @Value("${flutterwave.secret-key:}")
    private String secretKey;

    @Value("${flutterwave.enabled:false}")
    private boolean enabled;

    @Override
    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Initiating transfer to account: {} at bank: {}", request.getAccountNumber(), request.getBankCode());
        
        if (!enabled || secretKey == null || secretKey.isEmpty()) {
            log.warn("Flutterwave is not enabled or secret key is missing. Returning mock response.");
            return TransferResponse.builder()
                    .status("success")
                    .transactionRef("MOCK_TXN_" + System.currentTimeMillis())
                    .message("Mock transfer (Flutterwave not configured)")
                    .provider("flutterwave")
                    .build();
        }

        String url = baseUrl + "/transfers";
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("account_bank", request.getBankCode());
        payload.put("account_number", request.getAccountNumber());
        payload.put("amount", request.getAmount().intValue());
        payload.put("narration", request.getNarration());
        payload.put("currency", "NGN");
        payload.put("reference", "TXN_" + System.currentTimeMillis());
        payload.put("callback_url", request.getCallbackUrl());
        payload.put("beneficiary_name", request.getAccountName() != null ? request.getAccountName() : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(secretKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                String status = (String) body.getOrDefault("status", "error");
                Map<String, Object> data = (Map<String, Object>) body.getOrDefault("data", new HashMap<>());
                
                String transactionRef = (String) data.getOrDefault("reference", 
                        (String) body.getOrDefault("tx_ref", "TXN_" + System.currentTimeMillis()));
                String message = (String) body.getOrDefault("message", "");
                
                if ("success".equalsIgnoreCase(status)) {
                    return TransferResponse.builder()
                            .status("success")
                            .transactionRef(transactionRef)
                            .message(message)
                            .provider("flutterwave")
                            .build();
                } else {
                    log.error("Flutterwave transfer failed: {}", message);
                    throw new RuntimeException("Flutterwave transfer failed: " + message);
                }
            }
            
            throw new RuntimeException("Flutterwave transfer failed: Empty response");
        } catch (Exception e) {
            log.error("Error initiating Flutterwave transfer", e);
            throw new RuntimeException("Flutterwave transfer failed: " + e.getMessage(), e);
        }
    }
}
