package com.gigwave.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigwave.api.dto.reports.CreateReportRequest;
import com.gigwave.api.dto.reports.ReviewReportRequest;
import com.gigwave.application.reports.AccountReportService;
import com.gigwave.domain.reports.AccountReport;
import com.gigwave.domain.reports.ReportStatus;
import com.gigwave.domain.users.UserRole;
import com.gigwave.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountReportController.class)
class AccountReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountReportService accountReportService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockUuidUserSecurityContextFactory.class)
    @interface WithMockUuidUser {
        String roles() default "MUSICIAN";
        String userId() default "";
    }

    static class WithMockUuidUserSecurityContextFactory implements WithSecurityContextFactory<WithMockUuidUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockUuidUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            UUID userId = annotation.userId().isEmpty()
                    ? UUID.randomUUID()
                    : UUID.fromString(annotation.userId());
            String[] roles = annotation.roles().split(",");
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim())).toList()
            );
            context.setAuthentication(auth);
            return context;
        }
    }

    @Test
    @WithMockUuidUser(roles = "MUSICIAN")
    void testCreateReport_Success() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .reportedUserId(reportedUserId)
                .reason("Inappropriate behavior")
                .evidenceUrl("https://example.com/evidence.jpg")
                .build();

        AccountReport report = AccountReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .reason("Inappropriate behavior")
                .evidenceUrl("https://example.com/evidence.jpg")
                .status(ReportStatus.PENDING)
                .build();

        when(accountReportService.createReport(any(UUID.class), eq(reportedUserId), anyString(), anyString()))
                .thenReturn(report);

        mockMvc.perform(post("/api/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUuidUser(roles = "MUSICIAN")
    void testCreateReport_ValidationError() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .reason("") // Empty reason should fail validation
                .build();

        mockMvc.perform(post("/api/reports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUuidUser(roles = "MUSICIAN")
    void testGetMyReports() throws Exception {
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .reporterId(reporterId)
                .reportedUserId(reportedUserId)
                .status(ReportStatus.PENDING)
                .build();

        when(accountReportService.getReportsByReporter(any(UUID.class)))
                .thenReturn(Arrays.asList(report));

        mockMvc.perform(get("/api/reports/my-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUuidUser(roles = "ADMIN")
    void testGetPendingReports_Admin() throws Exception {
        AccountReport report = AccountReport.builder()
                .id(reportId)
                .status(ReportStatus.PENDING)
                .build();

        when(accountReportService.getPendingReports())
                .thenReturn(Arrays.asList(report));

        mockMvc.perform(get("/api/reports/admin/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUuidUser(roles = "MUSICIAN")
    void testGetPendingReports_NonAdmin() throws Exception {
        // Non-admin should get 403 Forbidden
        mockMvc.perform(get("/api/reports/admin/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUuidUser(roles = "ADMIN")
    void testReviewReport_Approve() throws Exception {
        ReviewReportRequest request = ReviewReportRequest.builder()
                .status(ReportStatus.APPROVED)
                .adminReview("Report approved, account disabled")
                .build();

        AccountReport report = AccountReport.builder()
                .id(reportId)
                .status(ReportStatus.APPROVED)
                .adminReview("Report approved, account disabled")
                .reviewedBy(adminId)
                .build();

        when(accountReportService.reviewReport(eq(reportId), any(UUID.class), any(ReportStatus.class), anyString()))
                .thenReturn(report);

        mockMvc.perform(post("/api/reports/admin/{reportId}/review", reportId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUuidUser(roles = "ADMIN")
    void testDisableAccount() throws Exception {
        mockMvc.perform(post("/api/reports/admin/users/{userId}/disable", reportedUserId)
                        .with(csrf())
                        .param("reason", "Violation of terms"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUuidUser(roles = "ADMIN")
    void testEnableAccount() throws Exception {
        mockMvc.perform(post("/api/reports/admin/users/{userId}/enable", reportedUserId)
                        .with(csrf())
                        .param("reason", "Account restored"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUuidUser(roles = "MUSICIAN")
    void testDisableAccount_NonAdmin() throws Exception {
        // Non-admin should get 403 Forbidden
        mockMvc.perform(post("/api/reports/admin/users/{userId}/disable", reportedUserId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}

