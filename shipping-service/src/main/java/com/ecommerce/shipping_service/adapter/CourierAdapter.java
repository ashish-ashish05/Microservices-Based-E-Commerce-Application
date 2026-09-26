package com.ecommerce.shipping_service.adapter;

import lombok.*;
import java.util.UUID;

public interface CourierAdapter {
    CourierResponse requestShipment(CourierRequest request);

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CourierRequest {
        private String address;
        private Double weight;
        private String method;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CourierResponse {
        private String trackingNumber;
        private boolean success;
        private String errorMessage;
    }
}
