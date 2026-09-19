package com.ecommerce.user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EmailAlreadyExistsException extends RuntimeException {
    private final String email;
    private final HttpStatus status = HttpStatus.CONFLICT;

    public EmailAlreadyExistsException(String email) {
        super("Email already in use");
        this.email = email;
    }
}
