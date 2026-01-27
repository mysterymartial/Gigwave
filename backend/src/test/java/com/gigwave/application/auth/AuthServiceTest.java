package com.gigwave.application.auth;

import com.gigwave.api.dto.auth.LoginRequest;
import com.gigwave.api.dto.auth.RegisterRequest;
import com.gigwave.application.users.UserService;
import com.gigwave.domain.users.User;
import com.gigwave.domain.users.UserRole;
import com.gigwave.infrastructure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserService userService;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .phone("08012345678")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.MUSICIAN)
                .build();
    }
    
    @Test
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setPhone("08012345678");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.MUSICIAN);
        
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(UUID.class), anyString())).thenReturn("test-token");
        
        // Act
        var response = authService.register(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals(userId, response.getUserId());
        verify(userService).registerUser(any(RegisterRequest.class));
        verify(jwtUtil).generateToken(userId, "MUSICIAN");
    }
    
    @Test
    void testRegister_WithNullEmail() {
        // Boundary: null email
        RegisterRequest request = new RegisterRequest();
        request.setPhone("08012345678");
        request.setEmail(null);
        request.setPassword("password123");
        request.setRole(UserRole.MUSICIAN);
        
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(UUID.class), anyString())).thenReturn("test-token");
        
        var response = authService.register(request);
        
        assertNotNull(response);
        verify(userService).registerUser(any(RegisterRequest.class));
    }
    
    @Test
    void testLogin_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setPhone("08012345678");
        request.setPassword("password123");
        
        when(userService.getUserByPhone("08012345678")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(userId, "MUSICIAN")).thenReturn("test-token");
        
        // Act
        var response = authService.login(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        verify(userService).getUserByPhone("08012345678");
        verify(passwordEncoder).matches("password123", "hashedPassword");
    }
    
    @Test
    void testLogin_InvalidPassword() {
        // Boundary: wrong password
        LoginRequest request = new LoginRequest();
        request.setPhone("08012345678");
        request.setPassword("wrongpassword");
        
        when(userService.getUserByPhone("08012345678")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        verify(userService).getUserByPhone("08012345678");
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
        verify(jwtUtil, never()).generateToken(any(), anyString());
    }
    
    @Test
    void testLogin_UserNotFound() {
        // Boundary: non-existent user
        LoginRequest request = new LoginRequest();
        request.setPhone("08099999999");
        request.setPassword("password123");
        
        when(userService.getUserByPhone("08099999999"))
                .thenThrow(new IllegalArgumentException("User not found"));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        verify(userService).getUserByPhone("08099999999");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
    
    @Test
    void testLogin_EmptyPhone() {
        // Boundary: empty phone
        LoginRequest request = new LoginRequest();
        request.setPhone("");
        request.setPassword("password123");
        
        when(userService.getUserByPhone(""))
                .thenThrow(new IllegalArgumentException("User not found"));
        
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
    
    @Test
    void testLogin_EmptyPassword() {
        // Boundary: empty password
        LoginRequest request = new LoginRequest();
        request.setPhone("08012345678");
        request.setPassword("");
        
        when(userService.getUserByPhone("08012345678")).thenReturn(testUser);
        when(passwordEncoder.matches("", "hashedPassword")).thenReturn(false);
        
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}
