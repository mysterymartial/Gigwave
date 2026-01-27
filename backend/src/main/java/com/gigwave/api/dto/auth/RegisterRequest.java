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
    
    @Email
    private String email;
    
    @NotBlank
    private String password;
    
    @NotNull
    private UserRole role;
}
