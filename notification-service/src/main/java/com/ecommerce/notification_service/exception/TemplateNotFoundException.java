package com.ecommerce.notification_service.exception;

public class TemplateNotFoundException extends NotificationException {
    public TemplateNotFoundException(String templateId) {
        super("Notification template not found: " + templateId);
    }
}
