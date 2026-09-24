package com.ecommerce.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "your-very-long-and-secure-secret-key-that-must-be-at-least-32-characters";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user123")
                .claim("role", "ADMIN")
                .signWith(key)
                .compact();

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void extractUserId_ValidToken_ReturnsSubject() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user123")
                .signWith(key)
                .compact();

        assertEquals("user123", jwtUtil.extractUserId(token));
    }

    @Test
    void extractRole_ValidToken_ReturnsRole() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user123")
                .claim("role", "ADMIN")
                .signWith(key)
                .compact();

        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }
}
