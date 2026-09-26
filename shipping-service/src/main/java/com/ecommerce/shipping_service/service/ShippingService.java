package com.ecommerce.shipping_service.service;

import com.ecommerce.shipping_service.adapter.CourierAdapter;
import com.ecommerce.shipping_service.client.TrackingServiceClient;
import com.ecommerce.shipping_service.domain.entity.*;
import com.ecommerce.shipping_service.domain.repository.*;
import com.ecommerce.shipping_service.dto.*;
import com.ecommerce.shipping_service.exception.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final CourierAdapter courierAdapter;
    private final TrackingServiceClient trackingServiceClient;

    @Transactional
    public ShipmentResponse createShipment(ShipmentRequest request) {
        log.info("Creating shipment for order: {}", request.getOrderId());

        // 1. Idempotency Check
        shipmentRepository.findByOrderId(request.getOrderId())
                .ifPresent(s -> {
                    throw new DuplicateShipmentException("Shipment already exists for order: " + request.getOrderId());
                });

        // 2. Method Validation
        if (!shippingMethodRepository.existsByIdAndActiveTrue(request.getShippingMethodId())) {
            throw new InvalidShippingMethodException("Shipping method not found or inactive: " + request.getShippingMethodId());
        }

        // 3. Local Persistence (Initial State)
        Shipment shipment = Shipment.builder()
                .orderId(request.getOrderId())
                .shippingMethodId(request.getShippingMethodId())
                .address(request.getShippingAddress())
                .status(ShipmentStatus.PENDING)
                .build();
        shipment = shipmentRepository.save(shipment);

        // 4. Courier Integration with Circuit Breaker
        CourierAdapter.CourierResponse courierResponse = callCourierService(request);

        if (courierResponse.isSuccess()) {
            shipment.setTrackingNumber(courierResponse.getTrackingNumber());
            shipment.setStatus(ShipmentStatus.SHIPPED);
            shipment = shipmentRepository.save(shipment);

            // 5. Remote Tracking Update
            updateTrackingService(shipment);
        } else {
            shipment.setStatus(ShipmentStatus.FAILED);
            shipmentRepository.save(shipment);
            throw new CourierIntegrationException("Courier failed to process shipment: " + courierResponse.getErrorMessage());
        }

        return mapToResponse(shipment);
    }

    @CircuitBreaker(name = "courierService", fallbackMethod = "fallbackCourierCall")
    private CourierAdapter.CourierResponse callCourierService(ShipmentRequest request) {
        return courierAdapter.requestShipment(CourierAdapter.CourierRequest.builder()
                .address(request.getShippingAddress())
                .weight(request.getWeight())
                .method("Standard") // Simplified for mock
                .build());
    }

    private CourierAdapter.CourierResponse fallbackCourierCall(ShipmentRequest request, Throwable t) {
        log.error("Circuit breaker triggered for courier service. Reason: {}", t.getMessage());
        return CourierAdapter.CourierResponse.builder()
                .success(false)
                .errorMessage("Courier service is currently unavailable. Shipment is pending assignment.")
                .build();
    }

    private void updateTrackingService(Shipment shipment) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("shipmentId", shipment.getId());
            data.put("trackingNumber", shipment.getTrackingNumber());
            data.put("status", shipment.getStatus());
            data.put("timestamp", LocalDateTime.now());
            trackingServiceClient.createTrackingRecord(data);
        } catch (Exception e) {
            log.error("Failed to update tracking service for shipment {}: {}", shipment.getId(), e.getMessage());
            // We don't fail the whole transaction as the shipment is already created and shipped
        }
    }

    public ShipmentResponse getShipmentDetails(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        return mapToResponse(shipment);
    }

    @Transactional
    public void updateShipmentStatus(UUID shipmentId, ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        // Validate status transition (cannot move backward)
        if (newStatus.ordinal() < shipment.getStatus().ordinal()) {
            throw new IllegalArgumentException("Cannot move shipment status backward from " + shipment.getStatus() + " to " + newStatus);
        }

        shipment.setStatus(newStatus);
        shipmentRepository.save(shipment);
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        return ShipmentResponse.builder()
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus().name())
                .estimatedDelivery(LocalDateTime.now().plusDays(5)) // Mock estimate
                .build();
    }
}
