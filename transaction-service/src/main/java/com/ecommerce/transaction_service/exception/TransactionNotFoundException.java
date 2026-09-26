package com.ecommerce.transaction_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TransactionNotFoundException extends RuntimeException {
    private final HttpStatus status = HttpStatus.NOT_FOUND;

    public TransactionNotFoundException(String message) {
        super(message);
    }
}
