package com.gigwave.api.controllers;

import com.gigwave.api.dto.kyc.KycDocumentDto;
import com.gigwave.application.kyc.KycService;
import com.gigwave.domain.users.KycDocument;
import com.gigwave.infrastructure.security.CurrentUser;
import com.gigwave.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    private final FileStorageService fileStorageService;
    private final KycService kycService;

    @PostMapping("/chat-media/{bookingId}")
    public ResponseEntity<Map<String, String>> uploadChatMedia(
            @PathVariable UUID bookingId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID userId
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String fileUrl = fileStorageService.storeChatMedia(file, bookingId.toString());
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "File uploaded successfully"));
        } catch (IOException e) {
            log.error("Error uploading chat media", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping("/dispute-evidence/{disputeId}")
    public ResponseEntity<Map<String, String>> uploadDisputeEvidence(
            @PathVariable UUID disputeId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID userId
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String fileUrl = fileStorageService.storeDisputeEvidence(file, disputeId.toString());
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "File uploaded successfully"));
        } catch (IOException e) {
            log.error("Error uploading dispute evidence", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping("/kyc-document")
    public ResponseEntity<KycDocumentDto> uploadKycDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @CurrentUser UUID userId
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            String fileUrl = fileStorageService.storeKycDocument(file, userId.toString());
            KycDocument document = kycService.submitKycDocument(userId, documentType, fileUrl);
            
            KycDocumentDto dto = KycDocumentDto.builder()
                    .id(document.getId())
                    .userId(document.getUserId())
                    .documentType(document.getDocumentType())
                    .documentUrl(document.getDocumentUrl())
                    .uploadedAt(document.getUploadedAt())
                    .build();
            
            return ResponseEntity.ok(dto);
        } catch (IOException e) {
            log.error("Error uploading KYC document", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/report-evidence")
    public ResponseEntity<Map<String, String>> uploadReportEvidence(
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID userId
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String fileUrl = fileStorageService.storeReportEvidence(file, userId.toString());
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "File uploaded successfully"));
        } catch (IOException e) {
            log.error("Error uploading report evidence", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping("/performance-video")
    public ResponseEntity<Map<String, String>> uploadPerformanceVideo(
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID userId
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file type is video
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be a video"));
            }

            String fileUrl = fileStorageService.storePerformanceVideo(file, userId.toString());
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "Video uploaded successfully"));
        } catch (IOException e) {
            log.error("Error uploading performance video", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload video: " + e.getMessage()));
        }
    }
}
