package com.gigwave.infrastructure.payments.onepipe;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OnePipeConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
