package com.gigwave.api.dto.gigs;

import com.gigwave.domain.gigs.GigStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GigDto {
    private UUID id;
    private UUID organizerId;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private Double latitude;
    private Double longitude;
    private String venuePictureUrl;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private GigStatus status;
    private LocalDateTime createdAt;
}


