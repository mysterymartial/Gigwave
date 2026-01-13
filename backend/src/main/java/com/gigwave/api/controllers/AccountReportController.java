package com.gigwave.api.controllers;

import com.gigwave.api.dto.reports.AccountReportDto;
import com.gigwave.api.dto.reports.CreateReportRequest;
import com.gigwave.api.dto.reports.ReviewReportRequest;
import com.gigwave.application.reports.AccountReportService;
import com.gigwave.domain.reports.AccountReport;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class AccountReportController {
    private final AccountReportService reportService;

    @PostMapping
    public ResponseEntity<AccountReportDto> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @CurrentUser UUID reporterId
    ) {
        AccountReport report = reportService.createReport(
                reporterId,
                request.getReportedUserId(),
                request.getReason(),
                request.getEvidenceUrl()
        );
        return ResponseEntity.ok(toDto(report));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<List<AccountReportDto>> getMyReports(@CurrentUser UUID userId) {
        List<AccountReport> reports = reportService.getReportsByReporter(userId);
        return ResponseEntity.ok(reports.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/reported-against-me")
    public ResponseEntity<List<AccountReportDto>> getReportsAgainstMe(@CurrentUser UUID userId) {
        List<AccountReport> reports = reportService.getReportsByReportedUser(userId);
        return ResponseEntity.ok(reports.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<AccountReportDto> getReport(@PathVariable UUID reportId) {
        AccountReport report = reportService.getReport(reportId);
        return ResponseEntity.ok(toDto(report));
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountReportDto>> getAllReports() {
        List<AccountReport> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountReportDto>> getPendingReports() {
        List<AccountReport> reports = reportService.getPendingReports();
        return ResponseEntity.ok(reports.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @PostMapping("/admin/{reportId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountReportDto> reviewReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request,
            @CurrentUser UUID adminId
    ) {
        AccountReport report = reportService.reviewReport(
                reportId,
                adminId,
                request.getStatus(),
                request.getAdminReview()
        );
        return ResponseEntity.ok(toDto(report));
    }

    @PostMapping("/admin/users/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableAccount(
            @PathVariable UUID userId,
            @RequestParam(required = false) String reason,
            @CurrentUser UUID adminId
    ) {
        reportService.disableAccount(userId, adminId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/users/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> enableAccount(
            @PathVariable UUID userId,
            @RequestParam(required = false) String reason,
            @CurrentUser UUID adminId
    ) {
        reportService.enableAccount(userId, adminId, reason);
        return ResponseEntity.ok().build();
    }

    private AccountReportDto toDto(AccountReport report) {
        return AccountReportDto.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .reportedUserId(report.getReportedUserId())
                .reason(report.getReason())
                .evidenceUrl(report.getEvidenceUrl())
                .status(report.getStatus())
                .adminReview(report.getAdminReview())
                .reviewedBy(report.getReviewedBy())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .build();
    }
}



