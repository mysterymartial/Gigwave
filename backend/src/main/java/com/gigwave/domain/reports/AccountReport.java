package com.gigwave.domain.reports;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "account_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReport {
    @Id
    private UUID id;

    @Indexed
    private UUID reporterId; // User who reported

    @Indexed
    private UUID reportedUserId; // User being reported

    private String reason;

    private String evidenceUrl; // Optional evidence (image/video/document)

    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    private String adminReview; // Admin's review/comments

    private UUID reviewedBy; // Admin who reviewed

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;
}



