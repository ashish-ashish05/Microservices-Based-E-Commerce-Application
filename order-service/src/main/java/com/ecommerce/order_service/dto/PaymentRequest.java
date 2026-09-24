package com.ecommerce.order_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private UUID orderId;
    private BigDecimal amount;
    private Map<String, Object> paymentDetails;
}
