package com.ecommerce.shipping_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequest {
    @NotNull(message = "orderId is required")
    private UUID orderId;

    @NotBlank(message = "shippingAddress is required")
    private String shippingAddress;

    @NotNull(message = "shippingMethodId is required")
    private UUID shippingMethodId;

    @Positive(message = "weight must be positive")
    private Double weight;
}
