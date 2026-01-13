package com.gigwave.infrastructure.persistence.reports;

import com.gigwave.domain.reports.AccountReport;
import com.gigwave.domain.reports.ReportStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountReportRepository extends MongoRepository<AccountReport, UUID> {
    List<AccountReport> findByReporterId(UUID reporterId);
    List<AccountReport> findByReportedUserId(UUID reportedUserId);
    List<AccountReport> findByStatus(ReportStatus status);
    List<AccountReport> findByReportedUserIdAndStatus(UUID reportedUserId, ReportStatus status);
}



