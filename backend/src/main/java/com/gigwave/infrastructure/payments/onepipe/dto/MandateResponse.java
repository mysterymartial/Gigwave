package com.gigwave.infrastructure.payments.onepipe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandateResponse {
    private String status;
    private String mandateRef;
    private String authorizationUrl;
    private String message;
}
