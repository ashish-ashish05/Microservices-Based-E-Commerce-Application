package com.ecommerce.shipping_service.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
public class MockCourierAdapter implements CourierAdapter {

    @Override
    public CourierResponse requestShipment(CourierRequest request) {
        log.info("Requesting shipment from mock courier for address: {}", request.getAddress());

        // Simulate API latency
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate occasional failure
        if (Math.random() < 0.05) {
            return CourierResponse.builder()
                    .success(false)
                    .errorMessage("Courier service unavailable")
                    .build();
        }

        return CourierResponse.builder()
                .success(true)
                .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
    }
}
