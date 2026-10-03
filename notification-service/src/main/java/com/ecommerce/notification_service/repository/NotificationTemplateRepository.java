package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
}
