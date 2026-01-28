package com.gigwave.infrastructure.storage;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import io.imagekit.sdk.models.BaseFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "file.storage.type", havingValue = "imagekit", matchIfMissing = true)
public class ImageKitFileStorageService implements FileStorageService {
    private final ImageKit imageKit;
    private final String urlEndpoint;

    public ImageKitFileStorageService(
            @Value("${imagekit.public-key}") String publicKey,
            @Value("${imagekit.private-key}") String privateKey,
            @Value("${imagekit.url-endpoint}") String urlEndpoint) {
        this.urlEndpoint = urlEndpoint;
        try {
            this.imageKit = ImageKit.getInstance();
            Configuration config = new Configuration(publicKey, privateKey, urlEndpoint);
            this.imageKit.setConfig(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ImageKit: " + e.getMessage(), e);
        }
    }

    @Override
    public String storeChatMedia(MultipartFile file, String bookingId) throws IOException {
        return storeFile(file, "chat-media", bookingId);
    }

    @Override
    public String storeDisputeEvidence(MultipartFile file, String disputeId) throws IOException {
        return storeFile(file, "dispute-evidence", disputeId);
    }

    @Override
    public String storeKycDocument(MultipartFile file, String userId) throws IOException {
        return storeFile(file, "kyc-documents", userId);
    }

    @Override
    public String storeReportEvidence(MultipartFile file, String userId) throws IOException {
        return storeFile(file, "report-evidence", userId);
    }

    @Override
    public String storePerformanceVideo(MultipartFile file, String userId) throws IOException {
        return storeFile(file, "performance-videos", userId);
    }

    @Override
    public String storeVenueImage(MultipartFile file, String userId) throws IOException {
        return storeFile(file, "venue-images", userId);
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        try {
            // Extract file ID from URL
            String fileId = extractFileIdFromUrl(fileUrl);
            if (fileId != null && !fileId.isEmpty()) {
                imageKit.deleteFile(fileId);
                log.info("Successfully deleted file from ImageKit: {}", fileUrl);
            } else {
                log.warn("Could not extract file ID from URL: {}. File may not exist in ImageKit.", fileUrl);
                // Don't throw exception for URLs that might be from local storage or other sources
            }
        } catch (Exception ex) {
            log.error("Error deleting file: {}", fileUrl, ex);
            throw new IOException("Could not delete file: " + fileUrl, ex);
        }
    }

    private String storeFile(MultipartFile file, String folder, String identifier) throws IOException {
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

        String fileName = folder + "_" + identifier + "_" + UUID.randomUUID() + fileExtension;

        try {
            FileCreateRequest fileCreateRequest = new FileCreateRequest(
                    file.getBytes(),
                    fileName
            );
            fileCreateRequest.setFolder(folder);
            fileCreateRequest.setUseUniqueFileName(true);
            List<String> tags = Arrays.asList(folder, identifier);
            fileCreateRequest.setTags(tags);

            Result result = imageKit.upload(fileCreateRequest);

            if (result.getUrl() != null) {
                return result.getUrl();
            } else {
                throw new IOException("ImageKit upload succeeded but no URL returned");
            }
        } catch (Exception ex) {
            log.error("Error uploading file to ImageKit", ex);
            throw new IOException("Could not upload file: " + ex.getMessage(), ex);
        }
    }

    private String extractFileIdFromUrl(String fileUrl) {
        // ImageKit URLs format: https://ik.imagekit.io/{urlEndpoint}/folder/filename.jpg
        // We need to query ImageKit to get the file ID from the URL
        if (fileUrl != null && fileUrl.contains(urlEndpoint)) {
            try {
                // Extract the path from the URL (everything after the endpoint)
                String path = fileUrl.substring(fileUrl.indexOf(urlEndpoint) + urlEndpoint.length());
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                
                // Query ImageKit by path to get file ID
                io.imagekit.sdk.models.GetFileListRequest fileListRequest = new io.imagekit.sdk.models.GetFileListRequest();
                fileListRequest.setPath(path);
                io.imagekit.sdk.models.results.ResultList resultList = imageKit.getFileList(fileListRequest);
                
                if (resultList.getResults() != null && resultList.getResults().size() > 0) {
                    BaseFile file = resultList.getResults().get(0);
                    return file.getFileId();
                }
            } catch (Exception e) {
                log.warn("Could not extract file ID from URL: {}", fileUrl, e);
            }
        }
        return null;
    }
}
