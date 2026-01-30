package com.gigwave.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Primary FileStorageService: tries ImageKit first, falls back to LocalFileStorageService on failure.
 * ImageKit is default; if it fails (e.g. network, config), local storage is used.
 */
@Service
@Primary
@Slf4j
public class FallbackFileStorageService implements FileStorageService {

    private final FileStorageService imageKit;
    private final FileStorageService local;

    public FallbackFileStorageService(
            @Autowired(required = false) ImageKitFileStorageService imageKit,
            @Autowired(required = false) LocalFileStorageService local) {
        this.imageKit = imageKit;
        this.local = local;
        if (this.local == null) {
            throw new IllegalStateException("LocalFileStorageService must be available as fallback");
        }
    }

    private String tryImageKitThenLocal(StorageOp op) throws IOException {
        if (imageKit != null) {
            try {
                return op.run(imageKit);
            } catch (Exception e) {
                log.warn("ImageKit storage failed, using local fallback: {}", e.getMessage());
            }
        }
        return op.run(local);
    }

    @Override
    public String storeChatMedia(MultipartFile file, String bookingId) throws IOException {
        return tryImageKitThenLocal((s) -> s.storeChatMedia(file, bookingId));
    }

    @Override
    public String storeDisputeEvidence(MultipartFile file, String disputeId) throws IOException {
        return tryImageKitThenLocal((s) -> s.storeDisputeEvidence(file, disputeId));
    }

    @Override
    public String storeKycDocument(MultipartFile file, String userId) throws IOException {
        return tryImageKitThenLocal((s) -> s.storeKycDocument(file, userId));
    }

    @Override
    public String storeReportEvidence(MultipartFile file, String userId) throws IOException {
        return tryImageKitThenLocal((s) -> s.storeReportEvidence(file, userId));
    }

    @Override
    public String storePerformanceVideo(MultipartFile file, String userId) throws IOException {
        return tryImageKitThenLocal((s) -> s.storePerformanceVideo(file, userId));
    }

    @Override
    public String storeVenueImage(MultipartFile file, String userId) throws IOException {
        return tryImageKitThenLocal((s) -> s.storeVenueImage(file, userId));
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        if (imageKit != null) {
            try {
                imageKit.deleteFile(fileUrl);
                return;
            } catch (Exception e) {
                log.warn("ImageKit delete failed, trying local: {}", e.getMessage());
            }
        }
        local.deleteFile(fileUrl);
    }

    @FunctionalInterface
    private interface StorageOp {
        String run(FileStorageService service) throws IOException;
    }
}
