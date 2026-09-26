package com.ecommerce.transaction_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    @NotNull
    private UUID orderId;

    @NotNull
    private UUID paymentId;

    private String providerTransactionId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private com.ecommerce.transaction_service.domain.TransactionStatus status;
}
