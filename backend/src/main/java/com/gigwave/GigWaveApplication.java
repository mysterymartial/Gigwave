package com.gigwave;

import com.gigwave.infrastructure.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GigWaveApplication {
    public static void main(String[] args) {
        EnvLoader.loadIfPresent();
        SpringApplication.run(GigWaveApplication.class, args);
    }
}
