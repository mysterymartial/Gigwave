package com.gigwave.domain.payments;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Document(collection = "idempotency_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    @Id
    private UUID id;

    @Indexed(unique = true)
    private String key;

    private String mandateRef;
    private String authorizationUrl;
    private String status;
    private String message;

    @Indexed(expireAfterSeconds = 0)
    private Date expiresAt;
}
