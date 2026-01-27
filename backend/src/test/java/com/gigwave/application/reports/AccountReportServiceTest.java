package com.gigwave.application.reports;

import com.gigwave.application.notifications.NotificationService;
import com.gigwave.domain.reports.AccountReport;
import com.gigwave.infrastructure.persistence.reports.AccountReportRepository;
import com.gigwave.domain.reports.ReportStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.domain.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountReportServiceTest {
    @Mock
    private AccountReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountReportService accountReportService;

    private UUID reporterId;
    private UUID reportedUserId;
    private UUID adminId;
    private UUID reportId;

    @BeforeEach
    void setUp() {
        reporterId = UUID.randomUUID();
        reportedUserId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        reportId = UUID.randomUUID();
    }

    @Test
    void testCreateReport_Success() {
        User reporter = User.builder().id(reporterId).isDisabled(false).build();
        User reported = User.builder().id(reportedUserId).build();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reported));
        when(reportRepository.save(any(AccountReport.class))).thenAnswer(invocation -> {
            AccountReport report = invocation.getArgument(0);
            report.setId(reportId);
            return report;
        });

        AccountReport result = accountReportService.createReport(reporterId, reportedUserId, "Inappropriate behavior", null);

        assertNotNull(result);
        assertEquals(reporterId, result.getReporterId());
        assertEquals(reportedUserId, result.getReportedUserId());
        assertEquals("Inappropriate behavior", result.getReason());
        assertEquals(ReportStatus.PENDING, result.getStatus());
        verify(reportRepository).save(any(AccountReport.class));
    }

    @Test
    void testCreateReport_WithEvidence() {
        User reporter = User.builder().id(reporterId).isDisabled(false).build();
        User reported = User.builder().id(reportedUserId).build();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reported));
        when(reportRepository.save(any(AccountReport.class))).thenAnswer(invocation -> {
            AccountReport report = invocation.getArgument(0);
            report.setId(reportId);
            return report;
        });

        String evidenceUrl = "https://example.com/evidence.jpg";
        AccountReport result = accountReportService.createReport(reporterId, reportedUserId, "Inappropriate behavior", evidenceUrl);

        assertEquals(evidenceUrl, result.getEvidenceUrl());
    }

    @Test
    void testCreateReport_CannotReportYourself() {
        // Edge case: cannot report your own account
        User user = User.builder().id(reporterId).isDisabled(false).build();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> accountReportService.createReport(reporterId, reporterId, "Reason", null),
                "Cannot report your own account");
    }

    @Test
    void testCreateReport_ReporterDisabled() {
        // Edge case: disabled account cannot create reports
        User disabledReporter = User.builder().id(reporterId).isDisabled(true).build();
        User reported = User.builder().id(reportedUserId).build();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(disabledReporter));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reported));

        assertThrows(IllegalArgumentException.class,
                () -> accountReportService.createReport(reporterId, reportedUserId, "Reason", null),
                "Cannot create report: your account is disabled");
    }

    @Test
    void testCreateReport_UserNotFound() {
        // Boundary: reporter not found
        when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> accountReportService.createReport(reporterId, reportedUserId, "Reason", null),
                "Reporter not found");
    }

    @Test
    void testReviewReport_Success_Approved() {
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .status(ReportStatus.PENDING)
                .build();

        User admin = User.builder().id(adminId).role(UserRole.ADMIN).build();
        User reportedUser = User.builder().id(reportedUserId).isDisabled(false).build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reportedUser));
        when(reportRepository.save(any(AccountReport.class))).thenReturn(report);

        AccountReport result = accountReportService.reviewReport(reportId, adminId, ReportStatus.APPROVED, "Approved by admin");

        assertEquals(ReportStatus.APPROVED, result.getStatus());
        assertEquals("Approved by admin", result.getAdminReview());
        assertEquals(adminId, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        assertTrue(reportedUser.getIsDisabled()); // Account should be disabled
        verify(userRepository).save(reportedUser);
    }

    @Test
    void testReviewReport_Success_Rejected() {
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .status(ReportStatus.PENDING)
                .build();

        User admin = User.builder().id(adminId).role(UserRole.ADMIN).build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(AccountReport.class))).thenReturn(report);

        AccountReport result = accountReportService.reviewReport(reportId, adminId, ReportStatus.REJECTED, "False report");

        assertEquals(ReportStatus.REJECTED, result.getStatus());
        assertEquals("False report", result.getAdminReview());
    }

    @Test
    void testReviewReport_NonAdminCannotReview() {
        // Edge case: only admins can review reports
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .status(ReportStatus.PENDING)
                .build();

        User nonAdmin = User.builder().id(reporterId).role(UserRole.MUSICIAN).build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(nonAdmin));

        assertThrows(IllegalArgumentException.class,
                () -> accountReportService.reviewReport(reportId, reporterId, ReportStatus.APPROVED, "Review"),
                "Only admins can review reports");
    }

    @Test
    void testDisableAccount_Success() {
        User admin = User.builder().id(adminId).role(UserRole.ADMIN).build();
        User userToDisable = User.builder().id(reportedUserId).isDisabled(false).build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(userToDisable));
        when(userRepository.save(any(User.class))).thenReturn(userToDisable);

        accountReportService.disableAccount(reportedUserId, adminId, "Violation of terms");

        assertTrue(userToDisable.getIsDisabled());
        verify(userRepository).save(userToDisable);
    }

    @Test
    void testDisableAccount_NonAdminCannotDisable() {
        User nonAdmin = User.builder().id(reporterId).role(UserRole.MUSICIAN).build();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(nonAdmin));

        assertThrows(IllegalArgumentException.class,
                () -> accountReportService.disableAccount(reportedUserId, reporterId, "Reason"),
                "Only admins can disable accounts");
    }

    @Test
    void testEnableAccount_Success() {
        User admin = User.builder().id(adminId).role(UserRole.ADMIN).build();
        User disabledUser = User.builder().id(reportedUserId).isDisabled(true).build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(any(User.class))).thenReturn(disabledUser);

        accountReportService.enableAccount(reportedUserId, adminId, "Account restored");

        assertFalse(disabledUser.getIsDisabled());
        verify(userRepository).save(disabledUser);
    }

    @Test
    void testGetPendingReports() {
        AccountReport report1 = AccountReport.builder().id(UUID.randomUUID()).status(ReportStatus.PENDING).build();
        AccountReport report2 = AccountReport.builder().id(UUID.randomUUID()).status(ReportStatus.PENDING).build();

        when(reportRepository.findByStatus(ReportStatus.PENDING)).thenReturn(Arrays.asList(report1, report2));

        List<AccountReport> result = accountReportService.getPendingReports();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == ReportStatus.PENDING));
    }

    @Test
    void testGetReportsByReporter() {
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .build();

        when(reportRepository.findByReporterId(reporterId)).thenReturn(Arrays.asList(report));

        List<AccountReport> result = accountReportService.getReportsByReporter(reporterId);

        assertEquals(1, result.size());
        assertEquals(reporterId, result.get(0).getReporterId());
    }

    @Test
    void testGetReportsByReportedUser() {
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .reportedUserId(reportedUserId)
                .build();

        when(reportRepository.findByReportedUserId(reportedUserId)).thenReturn(Arrays.asList(report));

        List<AccountReport> result = accountReportService.getReportsByReportedUser(reportedUserId);

        assertEquals(1, result.size());
        assertEquals(reportedUserId, result.get(0).getReportedUserId());
    }

    @Test
    void testGetReport_NotFound() {
        // Boundary: report not found
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> accountReportService.getReport(reportId),
                "Report not found");
    }

    @Test
    void testCreateReport_EmptyReason() {
        // Boundary: empty reason (though validation should catch this at controller level)
        User reporter = User.builder().id(reporterId).isDisabled(false).build();
        User reported = User.builder().id(reportedUserId).build();

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportedUserId)).thenReturn(Optional.of(reported));
        when(reportRepository.save(any(AccountReport.class))).thenAnswer(invocation -> {
            AccountReport report = invocation.getArgument(0);
            report.setId(reportId);
            return report;
        });
        doNothing().when(notificationService).sendDisputeRaisedNotification(any());

        // This should pass service layer but fail at validation
        AccountReport result = accountReportService.createReport(reporterId, reportedUserId, "", null);
        assertNotNull(result);
    }
}
