package com.gigwave.api.dto.reports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {
    @NotNull(message = "Reported user ID is required")
    private UUID reportedUserId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String evidenceUrl; // Optional
}
