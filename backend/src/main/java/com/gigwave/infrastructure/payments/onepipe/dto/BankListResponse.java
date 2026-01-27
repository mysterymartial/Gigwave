package com.gigwave.infrastructure.payments.onepipe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankListResponse {
    private List<Bank> banks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bank {
        private String code;
        private String name;
    }
}
