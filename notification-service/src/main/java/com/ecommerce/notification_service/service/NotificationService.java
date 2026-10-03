package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.domain.NotificationLog;
import com.ecommerce.notification_service.domain.NotificationLog.NotificationStatus;
import com.ecommerce.notification_service.domain.NotificationTemplate;
import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.exception.NotificationDeliveryException;
import com.ecommerce.notification_service.exception.TemplateNotFoundException;
import com.ecommerce.notification_service.repository.NotificationLogRepository;
import com.ecommerce.notification_service.repository.NotificationTemplateRepository;
import com.ecommerce.notification_service.strategy.NotificationStrategy;
import com.ecommerce.notification_service.strategy.NotificationStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationStrategyFactory strategyFactory;

    @Transactional
    public void processNotification(NotificationRequest request) {
        // 1. Idempotency Check
        Optional<NotificationLog> existingLog = logRepository.findByCorrelationId(request.getCorrelationId());
        if (existingLog.isPresent() && existingLog.get().getStatus() == NotificationStatus.SENT) {
            log.info("Notification already sent for correlationId: {}. Skipping.", request.getCorrelationId());
            return;
        }

        // 2. Log Initialization
        NotificationLog notificationLog = NotificationLog.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .templateId(request.getTemplateId())
                .status(NotificationStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .correlationId(request.getCorrelationId())
                .build();

        notificationLog = logRepository.save(notificationLog);

        // 3. Template Population
        NotificationTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new TemplateNotFoundException(request.getTemplateId()));

        String subject = populateTemplate(template.getSubject(), request.getTemplateParams());
        String body = populateTemplate(template.getBody(), request.getTemplateParams());

        // 4. Asynchronous Delivery
        sendAsynchronously(notificationLog.getId(), request.getRecipientEmail(), subject, body);
    }

    @Async
    public void sendAsynchronously(java.util.UUID logId, String recipient, String subject, String body) {
        try {
            // For now we default to EMAIL channel
            NotificationStrategy strategy = strategyFactory.getStrategy("EMAIL");
            strategy.send(recipient, subject, body);

            updateLogStatus(logId, NotificationStatus.SENT, null);
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", logId, e.getMessage());
            updateLogStatus(logId, NotificationStatus.FAILED, e.getMessage());
        }
    }

    private void updateLogStatus(java.util.UUID logId, NotificationStatus status, String error) {
        logRepository.findById(logId).ifPresent(log -> {
            log.setStatus(status);
            log.setErrorMessage(error);
            logRepository.save(log);
        });
    }

    private String populateTemplate(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
