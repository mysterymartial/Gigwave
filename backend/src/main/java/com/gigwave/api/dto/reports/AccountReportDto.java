package com.gigwave.api.dto.reports;

import com.gigwave.domain.reports.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReportDto {
    private UUID id;
    private UUID reporterId;
    private UUID reportedUserId;
    private String reason;
    private String evidenceUrl;
    private ReportStatus status;
    private String adminReview;
    private UUID reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}



