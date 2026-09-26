package com.ecommerce.payment_service.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.ecommerce.payment_service.domain.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
}
