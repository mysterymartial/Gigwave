package com.gigwave.domain.users;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "musician_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicianProfile {
    @Id
    private UUID userId;

    private String stageName;

    @Builder.Default
    private List<String> genres = new ArrayList<>();

    private String city;

    private BigDecimal minFee;

    @Builder.Default
    private Double rating = 0.0;

    // Social media handles
    private String instagramHandle;
    private String tiktokHandle;
    private String xHandle; // Twitter/X handle

    // Performance videos (at least one required)
    @Builder.Default
    private List<String> performanceVideoUrls = new ArrayList<>();
}
