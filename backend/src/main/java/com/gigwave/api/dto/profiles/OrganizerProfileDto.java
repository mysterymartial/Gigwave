package com.gigwave.api.dto.profiles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerProfileDto {
    private UUID userId;
    private String organizationName;
    private List<String> eventTypes;
    private String instagramHandle;
    private String tiktokHandle;
    private String xHandle;
}
