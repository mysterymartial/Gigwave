package com.gigwave.api.dto;

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
public class KycDocumentDto {
    private UUID id;
    private UUID userId;
    private String documentType;
    private String documentUrl;
    private LocalDateTime uploadedAt;
}
