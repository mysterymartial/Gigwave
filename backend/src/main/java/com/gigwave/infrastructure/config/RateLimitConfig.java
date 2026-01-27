package com.gigwave.infrastructure.config;

import io.github.bucket4j.BucketConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {
    @Bean
    public BucketConfiguration defaultBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(100)
                        .refillIntervally(100, Duration.ofMinutes(1))
                        .initialTokens(100))
                .build();
    }
}
