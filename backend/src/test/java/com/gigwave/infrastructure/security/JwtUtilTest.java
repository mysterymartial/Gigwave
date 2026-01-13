package com.gigwave.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private UUID testUserId;
    
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-minimum-32-characters-long-for-hmac");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
        testUserId = UUID.randomUUID();
    }
    
    @Test
    void testGenerateToken_Success() {
        String token = jwtUtil.generateToken(testUserId, "MUSICIAN");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void testExtractUserId_Success() {
        String token = jwtUtil.generateToken(testUserId, "MUSICIAN");
        UUID extracted = jwtUtil.extractUserId(token);
        
        assertEquals(testUserId, extracted);
    }
    
    @Test
    void testExtractRole_Success() {
        String token = jwtUtil.generateToken(testUserId, "EVENT_OWNER");
        String role = jwtUtil.extractRole(token);
        
        assertEquals("EVENT_OWNER", role);
    }
    
    @Test
    void testValidateToken_ValidToken() {
        String token = jwtUtil.generateToken(testUserId, "MUSICIAN");
        boolean isValid = jwtUtil.validateToken(token, testUserId);
        
        assertTrue(isValid);
    }
    
    @Test
    void testValidateToken_WrongUserId() {
        // Boundary: wrong user ID
        String token = jwtUtil.generateToken(testUserId, "MUSICIAN");
        UUID wrongUserId = UUID.randomUUID();
        boolean isValid = jwtUtil.validateToken(token, wrongUserId);
        
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_ExpiredToken() {
        // Boundary: expired token
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // Negative expiration
        String token = jwtUtil.generateToken(testUserId, "MUSICIAN");
        
        // Reset expiration
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        
        // Wait a bit and validate
        boolean isValid = jwtUtil.validateToken(token, testUserId);
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_InvalidToken() {
        // Boundary: invalid token format
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> jwtUtil.validateToken(invalidToken, testUserId));
    }
    
    @Test
    void testValidateToken_EmptyToken() {
        // Boundary: empty token
        assertThrows(Exception.class, () -> jwtUtil.validateToken("", testUserId));
    }
    
    @Test
    void testValidateToken_NullToken() {
        // Boundary: null token
        assertThrows(Exception.class, () -> jwtUtil.validateToken(null, testUserId));
    }
}





