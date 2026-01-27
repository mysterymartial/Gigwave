package com.gigwave.api.dto.reports;

import com.gigwave.domain.reports.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportRequest {
    @NotNull(message = "Status is required")
    private ReportStatus status;

    private String adminReview; // Optional admin comments
}
