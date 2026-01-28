package com.gigwave.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload.chat-media-dir:uploads/chat-media}")
    private String chatMediaDir;

    @Value("${file.upload.dispute-evidence-dir:uploads/dispute-evidence}")
    private String disputeEvidenceDir;

    @Value("${file.upload.kyc-document-dir:uploads/kyc-documents}")
    private String kycDocumentDir;

    @Value("${file.upload.report-evidence-dir:uploads/report-evidence}")
    private String reportEvidenceDir;

    @Value("${file.upload.performance-video-dir:uploads/performance-videos}")
    private String performanceVideoDir;

    @Value("${file.upload.venue-image-dir:uploads/venue-images}")
    private String venueImageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "file:" + chatMediaDir + "/",
                        "file:" + disputeEvidenceDir + "/",
                        "file:" + kycDocumentDir + "/",
                        "file:" + reportEvidenceDir + "/",
                        "file:" + performanceVideoDir + "/",
                        "file:" + venueImageDir + "/"
                );
    }
}
