package com.gigwave.api.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminDebitRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least ₦1")
    private BigDecimal amount;

    @NotBlank(message = "Reason is required")
    private String reason; // e.g., "Fraudulent activity", "Chargeback", etc.
}
