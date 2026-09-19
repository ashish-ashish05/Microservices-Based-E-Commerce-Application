package com.ecommerce.user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final String id;
    private final HttpStatus status = HttpStatus.NOT_FOUND;

    public UserNotFoundException(String id) {
        super("User not found");
        this.id = id;
    }
}
