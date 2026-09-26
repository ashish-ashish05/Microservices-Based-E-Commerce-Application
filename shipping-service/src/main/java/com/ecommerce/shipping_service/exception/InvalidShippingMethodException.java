package com.ecommerce.shipping_service.exception;

public class InvalidShippingMethodException extends RuntimeException {
    public InvalidShippingMethodException(String message) {
        super(message);
    }
}
