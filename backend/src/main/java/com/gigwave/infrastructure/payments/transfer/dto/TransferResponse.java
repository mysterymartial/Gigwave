package com.gigwave.infrastructure.payments.transfer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferResponse {
    private String status;
    private String transactionRef;
    private String message;
    private String provider; // e.g., "flutterwave", "paystack"
}
