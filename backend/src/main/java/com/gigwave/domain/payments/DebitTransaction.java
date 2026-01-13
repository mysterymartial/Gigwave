package com.gigwave.domain.payments;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "debit_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitTransaction {
    @Id
    private UUID id;

    private UUID bookingId;

    private UUID mandateId;

    private BigDecimal amount;

    private String providerRef;

    @Builder.Default
    private DebitStatus status = DebitStatus.PENDING;

    private LocalDateTime attemptedAt;

    private LocalDateTime completedAt;
}
