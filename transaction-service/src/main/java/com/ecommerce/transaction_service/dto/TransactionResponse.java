package com.ecommerce.transaction_service.dto;

import com.ecommerce.transaction_service.domain.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private UUID orderId;
    private UUID paymentId;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
