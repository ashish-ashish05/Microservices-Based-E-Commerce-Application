package com.ecommerce.notification_service.exception;

public class NotificationDeliveryException extends NotificationException {
    public NotificationDeliveryException(String message, Throwable cause) {
        super(message);
        // In a real app we might store the cause
    }
}
