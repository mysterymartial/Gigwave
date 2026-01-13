package com.gigwave.api.dto.profiles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicianProfileDto {
    private UUID userId;
    private String stageName;
    private List<String> genres;
    private String city;
    private BigDecimal minFee;
    private Double rating;
    private String instagramHandle;
    private String tiktokHandle;
    private String xHandle;
    private List<String> performanceVideoUrls;
}


