package com.gigwave.application.users;

import com.gigwave.domain.users.MusicianProfile;
import com.gigwave.infrastructure.persistence.users.MusicianProfileRepository;
import com.gigwave.domain.users.OrganizerProfile;
import com.gigwave.infrastructure.persistence.users.OrganizerProfileRepository;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final MusicianProfileRepository musicianProfileRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public MusicianProfile createOrUpdateMusicianProfile(
            UUID userId, 
            String stageName, 
            List<String> genres, 
            String city, 
            BigDecimal minFee,
            String instagramHandle,
            String tiktokHandle,
            String xHandle,
            List<String> performanceVideoUrls
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        MusicianProfile profile = musicianProfileRepository.findByUserId(userId)
                .orElse(MusicianProfile.builder().userId(userId).build());

        profile.setStageName(stageName);
        profile.setGenres(genres);
        profile.setCity(city);
        profile.setMinFee(minFee);
        profile.setInstagramHandle(instagramHandle);
        profile.setTiktokHandle(tiktokHandle);
        profile.setXHandle(xHandle);
        if (performanceVideoUrls != null) {
            // Enforce minimum 3 videos requirement
            if (performanceVideoUrls.size() < 3) {
                throw new IllegalArgumentException("Musician must upload at least 3 performance videos");
            }
            profile.setPerformanceVideoUrls(performanceVideoUrls);
        } else {
            // If not providing videos in update, check existing videos
            List<String> existingVideos = profile.getPerformanceVideoUrls();
            if (existingVideos == null || existingVideos.isEmpty()) {
                // New profile or profile with no videos - require at least 3
                throw new IllegalArgumentException("Musician must upload at least 3 performance videos");
            } else if (existingVideos.size() < 3) {
                // Existing profile with less than 3 videos - require at least 3
                throw new IllegalArgumentException("Musician must maintain at least 3 performance videos");
            }
            // If updating and not providing videos, keep existing ones (already validated above)
        }

        return musicianProfileRepository.save(profile);
    }

    public MusicianProfile getMusicianProfile(UUID userId) {
        return musicianProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Musician profile not found"));
    }

    @Transactional
    public OrganizerProfile createOrUpdateOrganizerProfile(
            UUID userId, 
            String organizationName, 
            List<String> eventTypes,
            String instagramHandle,
            String tiktokHandle,
            String xHandle
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        OrganizerProfile profile = organizerProfileRepository.findByUserId(userId)
                .orElse(OrganizerProfile.builder().userId(userId).build());

        profile.setOrganizationName(organizationName);
        profile.setEventTypes(eventTypes);
        profile.setInstagramHandle(instagramHandle);
        profile.setTiktokHandle(tiktokHandle);
        profile.setXHandle(xHandle);

        return organizerProfileRepository.save(profile);
    }

    public OrganizerProfile getOrganizerProfile(UUID userId) {
        return organizerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Organizer profile not found"));
    }
}


