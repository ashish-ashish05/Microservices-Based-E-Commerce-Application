package com.ecommerce.payment_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.ecommerce.payment_service.domain.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime updatedAt;
}
