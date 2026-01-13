package com.gigwave.application.reports;

import com.gigwave.domain.reports.AccountReport;
import com.gigwave.domain.reports.AccountReport;
import com.gigwave.domain.reports.ReportStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.reports.AccountReportRepository;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.domain.users.UserRole;
import com.gigwave.application.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountReportService {
    private final AccountReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public AccountReport createReport(UUID reporterId, UUID reportedUserId, String reason, String evidenceUrl) {
        // Verify users exist
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
        User reportedUser = userRepository.findById(reportedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Reported user not found"));

        // Cannot report yourself
        if (reporterId.equals(reportedUserId)) {
            throw new IllegalArgumentException("Cannot report your own account");
        }

        // Check if reporter's account is disabled
        if (Boolean.TRUE.equals(reporter.getIsDisabled())) {
            throw new IllegalArgumentException("Cannot create report: your account is disabled");
        }

        AccountReport report = AccountReport.builder()
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reason(reason)
                .evidenceUrl(evidenceUrl)
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);

        // Notify admins (in a real implementation)
        notificationService.sendDisputeRaisedNotification(null); // Could use booking ID if available

        return report;
    }

    @Transactional
    public AccountReport reviewReport(UUID reportId, UUID adminId, ReportStatus status, String adminReview) {
        AccountReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can review reports");
        }

        report.setStatus(status);
        report.setAdminReview(adminReview);
        report.setReviewedBy(adminId);
        report.setReviewedAt(LocalDateTime.now());

        // If approved, disable the reported user's account
        if (status == ReportStatus.APPROVED) {
            User reportedUser = userRepository.findById(report.getReportedUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Reported user not found"));
            reportedUser.setIsDisabled(true);
            userRepository.save(reportedUser);
        }

        return reportRepository.save(report);
    }

    @Transactional
    public void disableAccount(UUID userId, UUID adminId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can disable accounts");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsDisabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void enableAccount(UUID userId, UUID adminId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can enable accounts");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsDisabled(false);
        userRepository.save(user);
    }

    public List<AccountReport> getAllReports() {
        return reportRepository.findAll();
    }

    public List<AccountReport> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    public List<AccountReport> getReportsByReporter(UUID reporterId) {
        return reportRepository.findByReporterId(reporterId);
    }

    public List<AccountReport> getReportsByReportedUser(UUID reportedUserId) {
        return reportRepository.findByReportedUserId(reportedUserId);
    }

    public AccountReport getReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }
}

