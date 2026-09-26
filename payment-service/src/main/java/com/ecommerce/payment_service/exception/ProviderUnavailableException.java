package com.ecommerce.payment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class ProviderUnavailableException extends RuntimeException {
    public ProviderUnavailableException(String message) {
        super(message);
    }
}
