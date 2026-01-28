package com.gigwave.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String storeChatMedia(MultipartFile file, String bookingId) throws IOException;
    String storeDisputeEvidence(MultipartFile file, String disputeId) throws IOException;
    String storeKycDocument(MultipartFile file, String userId) throws IOException;
    String storeReportEvidence(MultipartFile file, String userId) throws IOException;
    String storePerformanceVideo(MultipartFile file, String userId) throws IOException;
    String storeVenueImage(MultipartFile file, String userId) throws IOException;
    void deleteFile(String fileUrl) throws IOException;
}
