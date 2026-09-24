package com.ecommerce.order_service.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRequest {
    private UUID orderId;
    private UUID userId;
    private String address;
}
