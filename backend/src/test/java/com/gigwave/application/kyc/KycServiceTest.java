package com.gigwave.application.kyc;

import com.gigwave.domain.users.KycDocument;
import com.gigwave.infrastructure.persistence.users.KycDocumentRepository;
import com.gigwave.domain.users.KycStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KycServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    
    @InjectMocks
    private KycService kycService;
    
    private UUID userId;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .kycStatus(KycStatus.PENDING)
                .build();
    }
    
    @Test
    void testSubmitKycDocument_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> {
            KycDocument doc = invocation.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        KycDocument result = kycService.submitKycDocument(
                userId, "NATIONAL_ID", "https://example.com/doc.jpg");
        
        assertNotNull(result);
        assertEquals("NATIONAL_ID", result.getDocumentType());
        verify(kycDocumentRepository).save(any(KycDocument.class));
    }
    
    @Test
    void testGetKycStatus_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        KycStatus result = kycService.getKycStatus(userId);
        
        assertEquals(KycStatus.PENDING, result);
    }
    
    @Test
    void testVerifyKyc_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        User result = kycService.verifyKyc(userId);
        
        assertEquals(KycStatus.VERIFIED, result.getKycStatus());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testRejectKyc_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        User result = kycService.rejectKyc(userId);
        
        assertEquals(KycStatus.REJECTED, result.getKycStatus());
    }
    
    @Test
    void testGetUserKycDocuments_Empty() {
        // Boundary: no documents
        when(kycDocumentRepository.findByUserId(userId)).thenReturn(Arrays.asList());
        
        List<KycDocument> result = kycService.getUserKycDocuments(userId);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetUserKycDocuments_Multiple() {
        KycDocument doc1 = KycDocument.builder().id(UUID.randomUUID()).build();
        KycDocument doc2 = KycDocument.builder().id(UUID.randomUUID()).build();
        
        when(kycDocumentRepository.findByUserId(userId))
                .thenReturn(Arrays.asList(doc1, doc2));
        
        List<KycDocument> result = kycService.getUserKycDocuments(userId);
        
        assertEquals(2, result.size());
    }
}
