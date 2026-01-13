package com.gigwave.api.dto.payments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDto {
    private UUID id;
    private UUID userId;
    private String bankName;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private Boolean isPayoutDefault;
}





