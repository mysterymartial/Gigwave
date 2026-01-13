package com.gigwave.infrastructure.payments.onepipe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitResponse {
    private String status;
    private String transactionRef;
    private String message;
    private String otpReference; // For OTP validation when status is WaitingForOTP
    private String validationUrl; // URL to validate OTP
}


