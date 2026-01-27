package com.gigwave.infrastructure.payments.onepipe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequest {
    private String accountNumber;
    private String bankCode;
    private String accountName;
    private BigDecimal amount;
    private String narration;
    private String callbackUrl;
    private java.util.UUID userId;
    private String email;
    private String phone;
}
