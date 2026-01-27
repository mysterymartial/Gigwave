package com.gigwave.application.users;

import com.gigwave.api.dto.auth.RegisterRequest;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import com.gigwave.domain.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private RegisterRequest registerRequest;
    
    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setPhone("08012345678");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(UserRole.MUSICIAN);
    }
    
    @Test
    void testRegisterUser_Success() {
        when(userRepository.existsByPhone("08012345678")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        
        User result = userService.registerUser(registerRequest);
        
        assertNotNull(result);
        assertEquals("08012345678", result.getPhone());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_DuplicatePhone() {
        // Boundary: duplicate phone number
        when(userRepository.existsByPhone("08012345678")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_DuplicateEmail() {
        // Boundary: duplicate email
        when(userRepository.existsByPhone("08012345678")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_WithNullEmail() {
        // Boundary: null email
        registerRequest.setEmail(null);
        when(userRepository.existsByPhone("08012345678")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        
        User result = userService.registerUser(registerRequest);
        
        assertNotNull(result);
        assertNull(result.getEmail());
    }
    
    @Test
    void testGetUserById_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).phone("08012345678").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        User result = userService.getUserById(userId);
        
        assertEquals(user, result);
    }
    
    @Test
    void testGetUserById_NotFound() {
        // Boundary: non-existent user
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(userId));
    }
    
    @Test
    void testGetUserByPhone_Success() {
        User user = User.builder().phone("08012345678").build();
        when(userRepository.findByPhone("08012345678")).thenReturn(Optional.of(user));
        
        User result = userService.getUserByPhone("08012345678");
        
        assertEquals(user, result);
    }
    
    @Test
    void testGetUserByPhone_NotFound() {
        // Boundary: non-existent phone
        when(userRepository.findByPhone("08099999999")).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByPhone("08099999999"));
    }
    
    @Test
    void testGetUserByPhone_EmptyPhone() {
        // Boundary: empty phone
        when(userRepository.findByPhone("")).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByPhone(""));
    }
}
