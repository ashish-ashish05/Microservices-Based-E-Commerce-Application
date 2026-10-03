package com.ecommerce.notification_service;

import com.ecommerce.notification_service.domain.NotificationLog;
import com.ecommerce.notification_service.domain.NotificationTemplate;
import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.repository.NotificationLogRepository;
import com.ecommerce.notification_service.repository.NotificationTemplateRepository;
import com.ecommerce.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class NotificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationLogRepository logRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        logRepository.deleteAll();
        templateRepository.deleteAll();

        NotificationTemplate welcomeTemplate = NotificationTemplate.builder()
                .id("WELCOME_EMAIL")
                .subject("Welcome to our Store, {{userName}}!")
                .body("Hello {{userName}}, thank you for joining us!")
                .description("Welcome email for new users")
                .build();
        templateRepository.save(welcomeTemplate);
    }

    @Test
    void testSendNotificationSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .recipientEmail("test@example.com")
                .templateId("WELCOME_EMAIL")
                .templateParams(Map.of("userName", "Ashish"))
                .correlationId(correlationId)
                .build();

        notificationService.processNotification(request);

        // Wait a bit for @Async task
        Thread.sleep(500);

        Optional<NotificationLog> log = logRepository.findByCorrelationId(correlationId);
        assertTrue(log.isPresent());
        assertEquals(NotificationLog.NotificationStatus.SENT, log.get().getStatus());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testIdempotency() throws Exception {
        UUID userId = UUID.randomUUID();
        String correlationId = "unique-id-123";

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .recipientEmail("test@example.com")
                .templateId("WELCOME_EMAIL")
                .templateParams(Map.of("userName", "Ashish"))
                .correlationId(correlationId)
                .build();

        notificationService.processNotification(request);
        Thread.sleep(500);
        notificationService.processNotification(request);
        Thread.sleep(500);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testTemplateNotFound() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .recipientEmail("test@example.com")
                .templateId("NON_EXISTENT")
                .templateParams(Map.of())
                .correlationId(UUID.randomUUID().toString())
                .build();

        assertThrows(com.ecommerce.notification_service.exception.TemplateNotFoundException.class, () -> {
            notificationService.processNotification(request);
        });
    }
}
