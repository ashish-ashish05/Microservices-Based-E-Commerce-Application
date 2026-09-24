package com.ecommerce.order_service.state;

import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.domain.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;

public class PaidState implements OrderState {
    @Override
    public void processPayment(Order order) {
        throw new InvalidOrderStateException("Order is already paid.");
    }

    @Override
    public void shipOrder(Order order) {
        order.setStatus(OrderStatus.SHIPPED);
    }

    @Override
    public void cancelOrder(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        // In a real system, this would trigger a refund command
    }

    @Override
    public void failOrder(Order order) {
        order.setStatus(OrderStatus.FAILED);
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PAID;
    }
}
