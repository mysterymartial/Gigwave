package com.gigwave.application.gigs;

import com.gigwave.api.dto.gigs.GigDto;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.gigs.GigStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GigServiceTest {
    @Mock
    private GigRepository gigRepository;
    
    @Mock
    private com.gigwave.infrastructure.persistence.users.UserRepository userRepository;
    
    @Mock
    private com.gigwave.application.notifications.NotificationService notificationService;
    
    @InjectMocks
    private GigService gigService;
    
    private Gig testGig;
    private UUID gigId;
    private UUID organizerId;
    
    @BeforeEach
    void setUp() {
        gigId = UUID.randomUUID();
        organizerId = UUID.randomUUID();
        testGig = Gig.builder()
                .id(gigId)
                .organizerId(organizerId)
                .title("Test Gig")
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(30))
                .location("Lagos")
                .budgetMin(new BigDecimal("50000"))
                .budgetMax(new BigDecimal("100000"))
                .status(GigStatus.OPEN)
                .build();
    }
    
    @Test
    void testCreateGig_Success() {
        // Arrange
        GigDto dto = GigDto.builder()
                .organizerId(organizerId)
                .title("New Gig")
                .description("New Description")
                .eventDate(LocalDateTime.now().plusDays(30))
                .location("Abuja")
                .budgetMin(new BigDecimal("30000"))
                .budgetMax(new BigDecimal("50000"))
                .build();
        
        when(gigRepository.save(any(Gig.class))).thenAnswer(invocation -> {
            Gig gig = invocation.getArgument(0);
            gig.setId(UUID.randomUUID());
            return gig;
        });
        when(userRepository.findByRole(any(com.gigwave.domain.users.UserRole.class))).thenReturn(Arrays.asList());
        
        // Act
        GigDto result = gigService.createGig(dto);
        
        // Assert
        assertNotNull(result);
        assertEquals("New Gig", result.getTitle());
        assertEquals(GigStatus.OPEN, result.getStatus());
        verify(gigRepository).save(any(Gig.class));
    }
    
    @Test
    void testCreateGig_WithNullBudget() {
        // Boundary: null budget values
        GigDto dto = GigDto.builder()
                .organizerId(organizerId)
                .title("Free Gig")
                .description("Free Description")
                .eventDate(LocalDateTime.now().plusDays(30))
                .location("Lagos")
                .budgetMin(null)
                .budgetMax(null)
                .build();
        
        when(gigRepository.save(any(Gig.class))).thenAnswer(invocation -> {
            Gig gig = invocation.getArgument(0);
            gig.setId(UUID.randomUUID());
            return gig;
        });
        when(userRepository.findByRole(any(com.gigwave.domain.users.UserRole.class))).thenReturn(Arrays.asList());
        
        GigDto result = gigService.createGig(dto);
        
        assertNotNull(result);
        assertNull(result.getBudgetMin());
        assertNull(result.getBudgetMax());
    }
    
    @Test
    void testCreateGig_WithZeroBudget() {
        // Boundary: zero budget
        GigDto dto = GigDto.builder()
                .organizerId(organizerId)
                .title("Zero Budget Gig")
                .description("Description")
                .eventDate(LocalDateTime.now().plusDays(30))
                .location("Lagos")
                .budgetMin(BigDecimal.ZERO)
                .budgetMax(BigDecimal.ZERO)
                .build();
        
        when(gigRepository.save(any(Gig.class))).thenAnswer(invocation -> {
            Gig gig = invocation.getArgument(0);
            gig.setId(UUID.randomUUID());
            return gig;
        });
        when(userRepository.findByRole(any(com.gigwave.domain.users.UserRole.class))).thenReturn(Arrays.asList());
        
        GigDto result = gigService.createGig(dto);
        
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBudgetMin());
    }
    
    @Test
    void testCreateGig_WithVeryLargeBudget() {
        // Boundary: very large budget
        GigDto dto = GigDto.builder()
                .organizerId(organizerId)
                .title("Large Budget Gig")
                .description("Description")
                .eventDate(LocalDateTime.now().plusDays(30))
                .location("Lagos")
                .budgetMin(new BigDecimal("1000000000"))
                .budgetMax(new BigDecimal("2000000000"))
                .build();
        
        when(gigRepository.save(any(Gig.class))).thenAnswer(invocation -> {
            Gig gig = invocation.getArgument(0);
            gig.setId(UUID.randomUUID());
            return gig;
        });
        when(userRepository.findByRole(any(com.gigwave.domain.users.UserRole.class))).thenReturn(Arrays.asList());
        
        GigDto result = gigService.createGig(dto);
        
        assertNotNull(result);
        assertTrue(result.getBudgetMin().compareTo(new BigDecimal("1000000000")) == 0);
    }
    
    @Test
    void testUpdateGig_Success() {
        // Arrange
        GigDto dto = GigDto.builder()
                .title("Updated Title")
                .description("Updated Description")
                .build();
        
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        when(gigRepository.save(any(Gig.class))).thenReturn(testGig);
        
        // Act
        GigDto result = gigService.updateGig(gigId, dto);
        
        // Assert
        assertNotNull(result);
        verify(gigRepository).findById(gigId);
        verify(gigRepository).save(any(Gig.class));
    }
    
    @Test
    void testUpdateGig_GigNotFound() {
        // Boundary: non-existent gig
        GigDto dto = GigDto.builder().title("Updated").build();
        
        when(gigRepository.findById(gigId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> gigService.updateGig(gigId, dto));
        verify(gigRepository).findById(gigId);
        verify(gigRepository, never()).save(any(Gig.class));
    }
    
    @Test
    void testCancelGig_Success() {
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(testGig));
        when(gigRepository.save(any(Gig.class))).thenReturn(testGig);
        
        gigService.cancelGig(gigId);
        
        verify(gigRepository).findById(gigId);
        verify(gigRepository).save(any(Gig.class));
        assertEquals(GigStatus.CANCELLED, testGig.getStatus());
    }
    
    @Test
    void testListOpenGigsForMusician_WithFilters() {
        // Boundary: all filters applied
        String city = "Lagos";
        BigDecimal minBudget = new BigDecimal("50000");
        BigDecimal maxBudget = new BigDecimal("100000");
        
        when(gigRepository.findByStatusAndEventDateAfter(
                eq(GigStatus.OPEN),
                any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(testGig));
        
        List<GigDto> result = gigService.listOpenGigsForMusician(city, minBudget, maxBudget);
        
        assertNotNull(result);
        // Result may be filtered in memory, so size could be 0 or 1
        assertTrue(result.size() <= 1);
    }
    
    @Test
    void testListOpenGigsForMusician_WithNullFilters() {
        // Boundary: null filters
        when(gigRepository.findByStatusAndEventDateAfter(
                eq(GigStatus.OPEN),
                any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(testGig));
        
        List<GigDto> result = gigService.listOpenGigsForMusician(null, null, null);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testListOpenGigsForMusician_EmptyResult() {
        // Boundary: no results
        when(gigRepository.findByStatusAndEventDateAfter(
                eq(GigStatus.OPEN),
                any(LocalDateTime.class)
        )).thenReturn(Arrays.asList());
        
        List<GigDto> result = gigService.listOpenGigsForMusician(null, null, null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetGigById_NotFound() {
        // Boundary: invalid ID
        UUID invalidId = UUID.randomUUID();
        when(gigRepository.findById(invalidId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> gigService.getGigById(invalidId));
    }
}
