package com.gigwave.api.controllers;

import com.gigwave.api.dto.profiles.MusicianProfileDto;
import com.gigwave.api.dto.profiles.OrganizerProfileDto;
import com.gigwave.application.users.ProfileService;
import com.gigwave.domain.users.MusicianProfile;
import com.gigwave.domain.users.OrganizerProfile;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping("/musician")
    @PreAuthorize("hasRole('MUSICIAN')")
    public ResponseEntity<MusicianProfileDto> createOrUpdateMusicianProfile(
            @Valid @RequestBody MusicianProfileDto dto,
            @CurrentUser UUID userId
    ) {
        MusicianProfile profile = profileService.createOrUpdateMusicianProfile(
                userId,
                dto.getStageName(),
                dto.getGenres(),
                dto.getCity(),
                dto.getMinFee(),
                dto.getInstagramHandle(),
                dto.getTiktokHandle(),
                dto.getXHandle(),
                dto.getPerformanceVideoUrls()
        );
        return ResponseEntity.ok(toMusicianDto(profile));
    }

    @GetMapping("/musician")
    @PreAuthorize("hasRole('MUSICIAN')")
    public ResponseEntity<MusicianProfileDto> getMusicianProfile(@CurrentUser UUID userId) {
        MusicianProfile profile = profileService.getMusicianProfile(userId);
        return ResponseEntity.ok(toMusicianDto(profile));
    }

    @PostMapping("/organizer")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<OrganizerProfileDto> createOrUpdateOrganizerProfile(
            @Valid @RequestBody OrganizerProfileDto dto,
            @CurrentUser UUID userId
    ) {
        OrganizerProfile profile = profileService.createOrUpdateOrganizerProfile(
                userId,
                dto.getOrganizationName(),
                dto.getEventTypes(),
                dto.getInstagramHandle(),
                dto.getTiktokHandle(),
                dto.getXHandle()
        );
        return ResponseEntity.ok(toOrganizerDto(profile));
    }

    @GetMapping("/organizer")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<OrganizerProfileDto> getOrganizerProfile(@CurrentUser UUID userId) {
        OrganizerProfile profile = profileService.getOrganizerProfile(userId);
        return ResponseEntity.ok(toOrganizerDto(profile));
    }

    @GetMapping("/organizer/{userId}")
    public ResponseEntity<OrganizerProfileDto> getOrganizerProfileByUserId(@PathVariable UUID userId) {
        OrganizerProfile profile = profileService.getOrganizerProfile(userId);
        return ResponseEntity.ok(toOrganizerDto(profile));
    }

    private MusicianProfileDto toMusicianDto(MusicianProfile profile) {
        return MusicianProfileDto.builder()
                .userId(profile.getUserId())
                .stageName(profile.getStageName())
                .genres(profile.getGenres())
                .city(profile.getCity())
                .minFee(profile.getMinFee())
                .rating(profile.getRating())
                .instagramHandle(profile.getInstagramHandle())
                .tiktokHandle(profile.getTiktokHandle())
                .xHandle(profile.getXHandle())
                .performanceVideoUrls(profile.getPerformanceVideoUrls())
                .build();
    }

    private OrganizerProfileDto toOrganizerDto(OrganizerProfile profile) {
        return OrganizerProfileDto.builder()
                .userId(profile.getUserId())
                .organizationName(profile.getOrganizationName())
                .eventTypes(profile.getEventTypes())
                .instagramHandle(profile.getInstagramHandle())
                .tiktokHandle(profile.getTiktokHandle())
                .xHandle(profile.getXHandle())
                .build();
    }
}
