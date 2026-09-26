package com.ecommerce.tracking_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TrackingNotFoundException extends RuntimeException {
    public TrackingNotFoundException(String trackingNumber) {
        super("Tracking information not found for number: " + trackingNumber);
    }
}
