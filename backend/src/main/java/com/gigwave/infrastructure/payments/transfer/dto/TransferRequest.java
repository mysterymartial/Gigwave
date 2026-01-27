package com.gigwave.infrastructure.payments.transfer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TransferRequest {
    private String accountNumber;
    private String bankCode;
    private String accountName;
    private BigDecimal amount;
    private String narration;
    private String callbackUrl;
    private UUID userId;
    private String email;
    private String phone;
}
