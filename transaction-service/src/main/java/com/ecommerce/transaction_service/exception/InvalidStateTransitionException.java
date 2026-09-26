package com.ecommerce.transaction_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidStateTransitionException extends RuntimeException {
    private final HttpStatus status = HttpStatus.BAD_REQUEST;

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
