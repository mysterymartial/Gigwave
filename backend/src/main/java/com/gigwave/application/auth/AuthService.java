package com.gigwave.application.auth;

import com.gigwave.api.dto.auth.AuthResponse;
import com.gigwave.api.dto.auth.LoginRequest;
import com.gigwave.api.dto.auth.RegisterRequest;
import com.gigwave.application.users.UserService;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = userService.registerUser(request);
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.getUserByPhone(request.getPhone());
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}





