package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    Optional<NotificationLog> findByCorrelationId(String correlationId);
    List<NotificationLog> findByUserId(UUID userId);
}
