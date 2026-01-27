package com.gigwave.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = false)
public class LocalFileStorageService implements FileStorageService {
    private final Path chatMediaStorageLocation;
    private final Path disputeEvidenceStorageLocation;
    private final Path kycDocumentStorageLocation;
    private final Path reportEvidenceStorageLocation;
    private final Path performanceVideoStorageLocation;

    public LocalFileStorageService(@Value("${file.upload.chat-media-dir:uploads/chat-media}") String chatMediaDir,
                                   @Value("${file.upload.dispute-evidence-dir:uploads/dispute-evidence}") String disputeEvidenceDir,
                                   @Value("${file.upload.kyc-document-dir:uploads/kyc-documents}") String kycDocumentDir,
                                   @Value("${file.upload.report-evidence-dir:uploads/report-evidence}") String reportEvidenceDir,
                                   @Value("${file.upload.performance-video-dir:uploads/performance-videos}") String performanceVideoDir) {
        this.chatMediaStorageLocation = Paths.get(chatMediaDir).toAbsolutePath().normalize();
        this.disputeEvidenceStorageLocation = Paths.get(disputeEvidenceDir).toAbsolutePath().normalize();
        this.kycDocumentStorageLocation = Paths.get(kycDocumentDir).toAbsolutePath().normalize();
        this.reportEvidenceStorageLocation = Paths.get(reportEvidenceDir).toAbsolutePath().normalize();
        this.performanceVideoStorageLocation = Paths.get(performanceVideoDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.chatMediaStorageLocation);
            Files.createDirectories(this.disputeEvidenceStorageLocation);
            Files.createDirectories(this.kycDocumentStorageLocation);
            Files.createDirectories(this.reportEvidenceStorageLocation);
            Files.createDirectories(this.performanceVideoStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeChatMedia(MultipartFile file, String bookingId) throws IOException {
        return storeFile(file, chatMediaStorageLocation, "chat", bookingId);
    }

    @Override
    public String storeDisputeEvidence(MultipartFile file, String disputeId) throws IOException {
        return storeFile(file, disputeEvidenceStorageLocation, "dispute", disputeId);
    }

    @Override
    public String storeKycDocument(MultipartFile file, String userId) throws IOException {
        return storeFile(file, kycDocumentStorageLocation, "kyc", userId);
    }

    @Override
    public String storeReportEvidence(MultipartFile file, String userId) throws IOException {
        return storeFile(file, reportEvidenceStorageLocation, "report", userId);
    }

    @Override
    public String storePerformanceVideo(MultipartFile file, String userId) throws IOException {
        return storeFile(file, performanceVideoStorageLocation, "performance", userId);
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        try {
            Path filePath = Paths.get(fileUrl).toAbsolutePath().normalize();
            Files.deleteIfExists(filePath);
        } catch (Exception ex) {
            log.error("Error deleting file: {}", fileUrl, ex);
            throw new IOException("Could not delete file: " + fileUrl, ex);
        }
    }

    private String storeFile(MultipartFile file, Path storageLocation, String prefix, String identifier) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("File name is empty");
        }

        // Check if file is empty
        if (file.isEmpty() || file.getSize() == 0) {
            throw new IOException("File is empty");
        }

        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }

        String fileName = prefix + "_" + identifier + "_" + UUID.randomUUID() + fileExtension;
        Path targetLocation = storageLocation.resolve(fileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Return relative URL path that matches the resource handler
        String folderName = storageLocation.getFileName().toString();
        return "/uploads/" + folderName + "/" + fileName;
    }
}
