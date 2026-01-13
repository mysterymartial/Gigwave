package com.gigwave.domain.payments;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Document(collection = "bank_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {
    @Id
    private UUID id;

    private UUID userId;

    private String bankName;

    private String bankCode;

    private String accountNumber;

    private String accountName;

    @Builder.Default
    private Boolean isPayoutDefault = false;
}
