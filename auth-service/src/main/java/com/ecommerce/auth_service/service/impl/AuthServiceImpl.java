package com.ecommerce.auth_service.service.impl;

import com.ecommerce.auth_service.client.RoleServiceClient;
import com.ecommerce.auth_service.dto.request.LoginRequest;
import com.ecommerce.auth_service.dto.request.TokenValidationRequest;
import com.ecommerce.auth_service.dto.response.LoginResponse;
import com.ecommerce.auth_service.dto.response.RoleResponse;
import com.ecommerce.auth_service.dto.response.TokenValidationResponse;
import com.ecommerce.auth_service.dto.response.UserResponse;
import com.ecommerce.auth_service.security.JwtProvider;
import com.ecommerce.auth_service.service.AuthService;
import com.ecommerce.auth_service.strategy.AuthenticationStrategy;
import com.ecommerce.auth_service.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserServiceClient userServiceClient;
    private final RoleServiceClient roleServiceClient;
    private final JwtProvider jwtProvider;
    private final AuthenticationStrategy authenticationStrategy;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. Authenticate using strategy
        if (authenticationStrategy.authenticate(request)) {
            // 2. Fetch user and roles for token
            UserResponse user = userServiceClient.getUserByUsername(request.getUsername());
            List<String> roles = new ArrayList<>();

            if (user.getRoleId() != null) {
                RoleResponse role = roleServiceClient.getRoleById(user.getRoleId());
                if (role != null) {
                    roles.add(role.getName());
                }
            }

            // 3. Generate JWT
            String token = jwtProvider.generateToken(user.getUsername(), roles);

            return LoginResponse.builder()
                    .token(token)
                    .expiresIn(expiration / 1000)
                    .build();
        }
        return null; // Should be handled by exception in strategy
    }

    @Override
    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        try {
            String token = request.getToken();
            if (jwtProvider.isTokenExpired(token)) {
                return TokenValidationResponse.builder().valid(false).build();
            }

            String username = jwtProvider.getUsername(token);
            List<String> roles = jwtProvider.getRoles(token);

            return TokenValidationResponse.builder()
                    .valid(true)
                    .username(username)
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder().valid(false).build();
        }
    }
}
