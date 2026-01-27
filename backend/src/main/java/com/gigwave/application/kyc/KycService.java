package com.gigwave.application.kyc;

import com.gigwave.domain.users.KycDocument;
import com.gigwave.infrastructure.persistence.users.KycDocumentRepository;
import com.gigwave.domain.users.KycStatus;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycService {
    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;

    @Transactional
    public KycDocument submitKycDocument(UUID userId, String documentType, String documentUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        KycDocument document = KycDocument.builder()
                .userId(userId)
                .documentType(documentType)
                .documentUrl(documentUrl)
                .build();

        return kycDocumentRepository.save(document);
    }

    public List<KycDocument> getUserKycDocuments(UUID userId) {
        return kycDocumentRepository.findByUserId(userId);
    }

    public KycStatus getKycStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getKycStatus();
    }

    @Transactional
    public User updateKycStatus(UUID userId, KycStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setKycStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public User verifyKyc(UUID userId) {
        return updateKycStatus(userId, KycStatus.VERIFIED);
    }

    @Transactional
    public User rejectKyc(UUID userId) {
        return updateKycStatus(userId, KycStatus.REJECTED);
    }
}
