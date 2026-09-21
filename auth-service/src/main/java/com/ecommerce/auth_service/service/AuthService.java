package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.request.LoginRequest;
import com.ecommerce.auth_service.dto.request.TokenValidationRequest;
import com.ecommerce.auth_service.dto.response.LoginResponse;
import com.ecommerce.auth_service.dto.response.TokenValidationResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    TokenValidationResponse validateToken(TokenValidationRequest request);
}
