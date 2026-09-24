package com.ecommerce.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class JwtAuthenticationFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    private String generateToken(String subject, String role) {
        String secret = "your-very-long-and-secure-secret-key-that-must-be-at-least-32-characters";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .signWith(key)
                .compact();
    }

    @Test
    void publicRoute_ShouldBeAccessibleWithoutToken() {
        webTestClient.get().uri("/auth/login")
                .exchange()
                .expectStatus().isNotFound(); // 404 because the service isn't actually running, but NOT 401
    }

    @Test
    void protectedRoute_WithoutToken_ShouldReturnUnauthorized() {
        webTestClient.get().uri("/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoute_WithInvalidToken_ShouldReturnUnauthorized() {
        webTestClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoute_WithValidToken_ShouldBeAllowed() {
        String token = generateToken("user123", "USER");
        webTestClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound(); // 404 because service isn't running, but NOT 401
    }
}
