package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.*;
import com.ecommerce.order_service.command.*;
import com.ecommerce.order_service.domain.*;
import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.state.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFulfillmentService {
    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final ShippingClient shippingClient;

    @Transactional
    public OrderResponse createOrder(UUID cartId, UUID userId, String address) {
        log.info("Starting order fulfillment for cart: {}, user: {}", cartId, userId);

        // 1. Fetch cart data - userId is String in CartResponse
        CartResponse cart = cartClient.getCart(userId.toString());
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 2. Initialize Order
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(cart.getTotalPrice())
                .status(OrderStatus.PENDING)
                .build();

        for (CartResponse.CartItemResponse item : cart.getItems()) {
            order.addOrderItem(OrderItem.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build());
        }

        order = orderRepository.save(order);

        // 3. Orchestrate Synchronous Saga
        Deque<OrderCommand> compensationStack = new ArrayDeque<>();
        try {
            // Step A: Reserve Inventory
            OrderCommand reserveInv = new ReserveInventoryCommand(inventoryClient, order.getItems());
            reserveInv.execute();
            compensationStack.push(reserveInv);

            // Step B: Process Payment
            OrderCommand processPay = new ProcessPaymentCommand(paymentClient, order);
            processPay.execute();

            // Transition to PAID
            OrderState state = OrderStateFactory.getState(order.getStatus());
            state.processPayment(order);
            orderRepository.save(order);
            compensationStack.push(processPay);

            // Step C: Create Shipment
            OrderCommand createShip = new CreateShippingCommand(shippingClient, order, address);
            createShip.execute();

            // Transition to SHIPPED
            state = OrderStateFactory.getState(order.getStatus());
            state.shipOrder(order);
            orderRepository.save(order);

            log.info("Order {} successfully fulfilled", order.getId());
            return mapToResponse(order);

        } catch (Exception e) {
            log.error("Order fulfillment failed for order {}: {}", order.getId(), e.getMessage());

            // Perform Compensation
            while (!compensationStack.isEmpty()) {
                OrderCommand cmd = compensationStack.pop();
                try {
                    cmd.undo();
                } catch (Exception undoEx) {
                    log.error("Compensation failed for command: {}", cmd.getClass().getSimpleName(), undoEx);
                }
            }

            // Transition to FAILED
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            throw e;
        }
    }

    public OrderResponse getOrderDetails(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.ecommerce.order_service.exception.OrderNotFoundException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.ecommerce.order_service.exception.OrderNotFoundException("Order not found: " + orderId));

        OrderState state = OrderStateFactory.getState(order.getStatus());
        state.cancelOrder(order);

        // If cancelled, release inventory
        for (OrderItem item : order.getItems()) {
            inventoryClient.releaseInventory(item.getProductId(), item.getQuantity());
        }

        orderRepository.save(order);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
