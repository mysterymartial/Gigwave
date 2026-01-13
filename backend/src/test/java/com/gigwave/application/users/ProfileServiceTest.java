package com.gigwave.application.users;

import com.gigwave.domain.users.MusicianProfile;
import com.gigwave.infrastructure.persistence.users.MusicianProfileRepository;
import com.gigwave.domain.users.OrganizerProfile;
import com.gigwave.infrastructure.persistence.users.OrganizerProfileRepository;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock
    private MusicianProfileRepository musicianProfileRepository;
    
    @Mock
    private OrganizerProfileRepository organizerProfileRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ProfileService profileService;
    
    private UUID userId;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder().id(userId).build();
    }
    
    @Test
    void testCreateOrUpdateMusicianProfile_New() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(musicianProfileRepository.save(any(MusicianProfile.class))).thenAnswer(invocation -> {
            MusicianProfile profile = invocation.getArgument(0);
            profile.setUserId(userId);
            return profile;
        });
        
        List<String> videos = Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4");
        MusicianProfile result = profileService.createOrUpdateMusicianProfile(
                userId, "Stage Name", Arrays.asList("Jazz", "Blues"), "Lagos", new BigDecimal("50000"),
                null, null, null, videos);
        
        assertNotNull(result);
        assertEquals("Stage Name", result.getStageName());
        assertEquals(3, result.getPerformanceVideoUrls().size());
        verify(musicianProfileRepository).save(any(MusicianProfile.class));
    }
    
    @Test
    void testCreateOrUpdateMusicianProfile_Update() {
        MusicianProfile existing = MusicianProfile.builder()
                .userId(userId)
                .stageName("Old Name")
                .performanceVideoUrls(Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4"))
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(musicianProfileRepository.save(any(MusicianProfile.class))).thenReturn(existing);
        
        List<String> newVideos = Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4", "video4.mp4");
        MusicianProfile result = profileService.createOrUpdateMusicianProfile(
                userId, "New Name", Arrays.asList("Rock"), "Abuja", new BigDecimal("60000"),
                null, null, null, newVideos);
        
        assertEquals("New Name", result.getStageName());
    }

    @Test
    void testCreateOrUpdateMusicianProfile_FailsWithLessThan3Videos() {
        // Boundary: Less than minimum required videos
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        List<String> insufficientVideos = Arrays.asList("video1.mp4", "video2.mp4");
        
        assertThrows(IllegalArgumentException.class,
                () -> profileService.createOrUpdateMusicianProfile(
                        userId, "Stage Name", Arrays.asList("Jazz"), "Lagos", new BigDecimal("50000"),
                        null, null, null, insufficientVideos),
                "Musician must upload at least 3 performance videos");
    }

    @Test
    void testCreateOrUpdateMusicianProfile_FailsWithNullVideosAndNoExisting() {
        // Boundary: Null videos for new profile
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> profileService.createOrUpdateMusicianProfile(
                        userId, "Stage Name", Arrays.asList("Jazz"), "Lagos", new BigDecimal("50000"),
                        null, null, null, null),
                "Musician must upload at least 3 performance videos");
    }

    @Test
    void testCreateOrUpdateMusicianProfile_FailsWithExistingVideosLessThan3() {
        // Boundary: Existing profile with less than 3 videos
        MusicianProfile existing = MusicianProfile.builder()
                .userId(userId)
                .stageName("Old Name")
                .performanceVideoUrls(Arrays.asList("video1.mp4")) // Only 1 video
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        
        assertThrows(IllegalArgumentException.class,
                () -> profileService.createOrUpdateMusicianProfile(
                        userId, "New Name", Arrays.asList("Rock"), "Abuja", new BigDecimal("60000"),
                        null, null, null, null), // Not providing videos, should check existing
                "Musician must maintain at least 3 performance videos");
    }

    @Test
    void testCreateOrUpdateMusicianProfile_SuccessWithExactly3Videos() {
        // Boundary: Exactly 3 videos (minimum required)
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(musicianProfileRepository.save(any(MusicianProfile.class))).thenAnswer(invocation -> {
            MusicianProfile profile = invocation.getArgument(0);
            profile.setUserId(userId);
            return profile;
        });
        
        List<String> exactlyThreeVideos = Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4");
        MusicianProfile result = profileService.createOrUpdateMusicianProfile(
                userId, "Stage Name", Arrays.asList("Jazz"), "Lagos", new BigDecimal("50000"),
                null, null, null, exactlyThreeVideos);
        
        assertNotNull(result);
        assertEquals(3, result.getPerformanceVideoUrls().size());
    }

    @Test
    void testCreateOrUpdateMusicianProfile_SuccessWithMoreThan3Videos() {
        // Boundary: More than minimum required videos
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(musicianProfileRepository.save(any(MusicianProfile.class))).thenAnswer(invocation -> {
            MusicianProfile profile = invocation.getArgument(0);
            profile.setUserId(userId);
            return profile;
        });
        
        List<String> fiveVideos = Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4", "video4.mp4", "video5.mp4");
        MusicianProfile result = profileService.createOrUpdateMusicianProfile(
                userId, "Stage Name", Arrays.asList("Jazz"), "Lagos", new BigDecimal("50000"),
                null, null, null, fiveVideos);
        
        assertNotNull(result);
        assertEquals(5, result.getPerformanceVideoUrls().size());
    }
    
    @Test
    void testCreateOrUpdateMusicianProfile_EmptyGenres() {
        // Boundary: empty genres list
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(musicianProfileRepository.save(any(MusicianProfile.class))).thenAnswer(invocation -> {
            MusicianProfile profile = invocation.getArgument(0);
            profile.setUserId(userId);
            return profile;
        });
        
        List<String> videos = Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4");
        MusicianProfile result = profileService.createOrUpdateMusicianProfile(
                userId, "Stage Name", Arrays.asList(), "Lagos", new BigDecimal("50000"),
                null, null, null, videos);
        
        assertNotNull(result);
        assertTrue(result.getGenres().isEmpty());
    }
    
    @Test
    void testCreateOrUpdateMusicianProfile_NullMinFee() {
        // Boundary: null min fee
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(musicianProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(musicianProfileRepository.save(any(MusicianProfile.class))).thenAnswer(invocation -> {
            MusicianProfile profile = invocation.getArgument(0);
            profile.setUserId(userId);
            return profile;
        });
        
        List<String> videos = Arrays.asList("video1.mp4", "video2.mp4", "video3.mp4");
        MusicianProfile result = profileService.createOrUpdateMusicianProfile(
                userId, "Stage Name", Arrays.asList("Jazz"), "Lagos", null,
                null, null, null, videos);
        
        assertNull(result.getMinFee());
    }
}



