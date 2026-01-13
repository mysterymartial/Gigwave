package com.gigwave.domain.users;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "kyc_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument {
    @Id
    private UUID id;

    private UUID userId;

    private String documentType; // e.g., "NATIONAL_ID", "PASSPORT", "DRIVERS_LICENSE"

    private String documentUrl;

    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
