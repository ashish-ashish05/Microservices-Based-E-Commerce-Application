package com.ecommerce.shipping_service.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private UUID shipmentId;
    private String trackingNumber;
    private String status;
    private LocalDateTime estimatedDelivery;
}
