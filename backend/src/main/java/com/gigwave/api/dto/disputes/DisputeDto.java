package com.gigwave.api.dto.disputes;

import com.gigwave.domain.disputes.DisputeStatus;
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
public class DisputeDto {
    private UUID id;
    private UUID bookingId;
    private UUID raisedBy;
    private String reason;
    private String evidenceUrl;
    private DisputeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}





