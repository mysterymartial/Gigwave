package com.gigwave.domain.users;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "organizer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerProfile {
    @Id
    private UUID userId;

    private String organizationName;

    @Builder.Default
    private List<String> eventTypes = new ArrayList<>();

    // Social media handles
    private String instagramHandle;
    private String tiktokHandle;
    private String xHandle; // Twitter/X handle
}
