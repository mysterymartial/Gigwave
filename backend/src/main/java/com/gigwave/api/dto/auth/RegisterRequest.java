package com.gigwave.api.dto.auth;

import com.gigwave.domain.users.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String phone;
    
    @Email(message = "Must be a valid email address")
    private String email; // optional; omit or null when not provided
    
    @NotBlank
    private String password;
    
    @NotNull
    private UserRole role;
}
