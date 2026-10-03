package com.ecommerce.notification_service.strategy;

import com.ecommerce.notification_service.domain.NotificationTemplate;
import java.util.Map;

public interface NotificationStrategy {
    void send(String recipient, String subject, String body);
    String getChannel();
}
