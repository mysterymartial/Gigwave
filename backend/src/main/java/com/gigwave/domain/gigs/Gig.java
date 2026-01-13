package com.gigwave.domain.gigs;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "gigs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gig {
    @Id
    private UUID id;

    private UUID organizerId;

    private String title;

    private String description;

    private LocalDateTime eventDate;

    private String location; // Address/venue name

    // Location coordinates for Google Maps
    private Double latitude;
    private Double longitude;

    // Venue picture URL
    private String venuePictureUrl;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    @Builder.Default
    private GigStatus status = GigStatus.OPEN;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
