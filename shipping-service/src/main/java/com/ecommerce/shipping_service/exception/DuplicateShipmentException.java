package com.ecommerce.shipping_service.exception;

public class DuplicateShipmentException extends RuntimeException {
    public DuplicateShipmentException(String message) {
        super(message);
    }
}
