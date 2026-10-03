package com.ecommerce.notification_service.controller;

import com.ecommerce.notification_service.domain.NotificationLog;
import com.ecommerce.notification_service.dto.NotificationRequest;
import com.ecommerce.notification_service.dto.NotificationResponse;
import com.ecommerce.notification_service.repository.NotificationLogRepository;
import com.ecommerce.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationLogRepository logRepository;

    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(@Valid @RequestBody NotificationRequest request) {
        notificationService.processNotification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<NotificationResponse>> getHistory(@PathVariable UUID userId) {
        List<NotificationResponse> history = logRepository.findByUserId(userId).stream()
                .map(log -> NotificationResponse.builder()
                        .id(log.getId())
                        .recipientEmail(log.getRecipientEmail())
                        .status(log.getStatus().name())
                        .sentAt(log.getSentAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }
}
