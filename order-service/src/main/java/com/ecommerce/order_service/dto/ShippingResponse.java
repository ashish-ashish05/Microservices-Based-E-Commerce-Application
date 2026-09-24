package com.ecommerce.order_service.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingResponse {
    private UUID shipmentId;
    private String trackingNumber;
    private String status;
}
