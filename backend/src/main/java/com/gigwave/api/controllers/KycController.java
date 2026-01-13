package com.gigwave.api.controllers;

import com.gigwave.api.dto.kyc.KycDocumentDto;
import com.gigwave.application.kyc.KycService;
import com.gigwave.domain.users.KycDocument;
import com.gigwave.domain.users.KycStatus;
import com.gigwave.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {
    private final KycService kycService;

    @PostMapping("/documents")
    public ResponseEntity<KycDocumentDto> submitKycDocument(
            @RequestParam String documentType,
            @RequestParam String documentUrl,
            @CurrentUser UUID userId
    ) {
        KycDocument document = kycService.submitKycDocument(userId, documentType, documentUrl);
        return ResponseEntity.ok(toDto(document));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KycDocumentDto>> getUserKycDocuments(@CurrentUser UUID userId) {
        List<KycDocument> documents = kycService.getUserKycDocuments(userId);
        return ResponseEntity.ok(documents.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/status")
    public ResponseEntity<KycStatus> getKycStatus(@CurrentUser UUID userId) {
        KycStatus status = kycService.getKycStatus(userId);
        return ResponseEntity.ok(status);
    }

    @PutMapping("/{userId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> verifyKyc(@PathVariable UUID userId) {
        kycService.verifyKyc(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectKyc(@PathVariable UUID userId) {
        kycService.rejectKyc(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateKycStatus(
            @PathVariable UUID userId,
            @RequestParam KycStatus status
    ) {
        kycService.updateKycStatus(userId, status);
        return ResponseEntity.ok().build();
    }

    private KycDocumentDto toDto(KycDocument document) {
        return KycDocumentDto.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .documentType(document.getDocumentType())
                .documentUrl(document.getDocumentUrl())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}





