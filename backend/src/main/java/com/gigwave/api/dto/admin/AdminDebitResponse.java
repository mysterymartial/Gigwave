package com.gigwave.api.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminDebitResponse {
    private UUID debitTransactionId;
    private UUID customerId;
    private BigDecimal amount;
    private String status;
    private String transactionRef;
    private String message;
    private LocalDateTime attemptedAt;
}
