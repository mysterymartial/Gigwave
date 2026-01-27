package com.gigwave.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigwave.api.dto.auth.LoginRequest;
import com.gigwave.api.dto.auth.RegisterRequest;
import com.gigwave.application.auth.AuthService;
import com.gigwave.api.dto.auth.AuthResponse;
import com.gigwave.domain.users.UserRole;
import com.gigwave.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import com.gigwave.infrastructure.security.JwtAuthenticationFilter;
import com.gigwave.infrastructure.security.SecurityHeadersFilter;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class, SecurityHeadersFilter.class}
    )
)
@Import(AuthControllerTest.TestSecurityConfig.class)
public class AuthControllerTest {
    
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private JwtUtil jwtUtil;
    
    @MockBean
    private com.gigwave.infrastructure.persistence.users.UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setPhone("08012345678");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.MUSICIAN);
        
        AuthResponse response = AuthResponse.builder()
                .token("test-token")
                .userId(UUID.randomUUID())
                .phone("08012345678")
                .role(UserRole.MUSICIAN)
                .build();
        
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.role").value("MUSICIAN"));
    }
    
    @Test
    void testRegister_InvalidRequest() throws Exception {
        // Boundary: missing required fields
        RegisterRequest request = new RegisterRequest();
        request.setPhone("");
        request.setPassword("");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPhone("08012345678");
        request.setPassword("password123");
        
        AuthResponse response = AuthResponse.builder()
                .token("test-token")
                .userId(UUID.randomUUID())
                .phone("08012345678")
                .role(UserRole.MUSICIAN)
                .build();
        
        when(authService.login(any(LoginRequest.class))).thenReturn(response);
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
    
    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Boundary: invalid credentials
        LoginRequest request = new LoginRequest();
        request.setPhone("08012345678");
        request.setPassword("wrongpassword");
        
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
