package com.ecommerce.auth_service.strategy.impl;

import com.ecommerce.auth_service.client.UserServiceClient;
import com.ecommerce.auth_service.dto.request.LoginRequest;
import com.ecommerce.auth_service.dto.response.UserResponse;
import com.ecommerce.auth_service.exception.InvalidCredentialsException;
import com.ecommerce.auth_service.security.PasswordHasher;
import com.ecommerce.auth_service.strategy.AuthenticationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

    private final UserServiceClient userServiceClient;
    private final PasswordHasher passwordHasher;

    @Override
    public boolean authenticate(LoginRequest request) {
        try {
            UserResponse user = userServiceClient.getUserByUsername(request.getUsername());
            if (user == null || !passwordHasher.verifyPassword(request.getPassword(), user.getPassword())) {
                throw new InvalidCredentialsException();
            }
            return true;
        } catch (Exception e) {
            if (e instanceof InvalidCredentialsException) throw (InvalidCredentialsException) e;
            throw new InvalidCredentialsException();
        }
    }

    @Override
    public String getUsername(LoginRequest request) {
        return request.getUsername();
    }
}
