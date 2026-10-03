package com.ecommerce.notification_service.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String recipientEmail;
    private String status;
    private LocalDateTime sentAt;
}
