package com.ecommerce.auth_service.exception;

public class TokenExpiredException extends AuthException {
    public TokenExpiredException() {
        super("Token has expired");
    }
}
