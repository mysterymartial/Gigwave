package com.gigwave.api.dto.admin;

import com.gigwave.domain.users.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerDto {
    private UUID id;
    private String phone;
    private String email;
    private UserRole role;
    private Boolean isDisabled;
    private LocalDateTime createdAt;
    private Boolean hasActiveMandate;
    private String mandateRef;
}
