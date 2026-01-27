package com.gigwave.domain.users;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private UUID id;

    @Indexed(unique = true)
    private String phone;

    @Indexed(unique = true, sparse = true)
    private String email;

    private String passwordHash;

    private UserRole role;

    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Builder.Default
    private Boolean isDisabled = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
