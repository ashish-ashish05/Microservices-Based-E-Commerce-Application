package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.service.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderFulfillmentService fulfillmentService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // In a real app, userId and address would come from the SecurityContext (JWT)
        // Using dummy values for now
        UUID userId = UUID.randomUUID();
        String address = "123 Main St, Springfield";

        OrderResponse response = fulfillmentService.createOrder(request.getCartId(), userId, address);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(fulfillmentService.getOrderDetails(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId) {
        fulfillmentService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
