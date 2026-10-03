package com.ecommerce.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    @Email
    private String recipientEmail;

    @NotBlank
    private String templateId;

    @NotNull
    private Map<String, String> templateParams;

    @NotBlank
    private String correlationId;
}
