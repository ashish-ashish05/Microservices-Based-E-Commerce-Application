package com.ecommerce.notification_service.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false, unique = true)
    private String correlationId;

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
