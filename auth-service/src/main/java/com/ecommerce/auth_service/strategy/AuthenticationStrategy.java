package com.ecommerce.auth_service.strategy;

import com.ecommerce.auth_service.dto.request.LoginRequest;

public interface AuthenticationStrategy {
    boolean authenticate(LoginRequest request);
    String getUsername(LoginRequest request);
}
