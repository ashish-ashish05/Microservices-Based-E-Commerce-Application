package com.ecommerce.order_service.state;

import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.domain.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;

public class PendingState implements OrderState {
    @Override
    public void processPayment(Order order) {
        order.setStatus(OrderStatus.PAID);
    }

    @Override
    public void shipOrder(Order order) {
        throw new InvalidOrderStateException("Cannot ship order in PENDING state. Payment must be processed first.");
    }

    @Override
    public void cancelOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
    }

    @Override
    public void failOrder(Order order) {
        order.setStatus(OrderStatus.FAILED);
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PENDING;
    }
}
