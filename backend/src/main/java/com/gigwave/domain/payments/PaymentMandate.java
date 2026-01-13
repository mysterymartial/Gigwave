package com.gigwave.domain.payments;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "payment_mandates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMandate {
    @Id
    private UUID id;

    private UUID userId;

    private UUID bankAccountId;

    private String provider;

    @Indexed(unique = true, sparse = true)
    private String mandateRef;

    @Builder.Default
    private MandateStatus status = MandateStatus.PENDING;

    private BigDecimal maxAmount;

    private LocalDateTime expiresAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
