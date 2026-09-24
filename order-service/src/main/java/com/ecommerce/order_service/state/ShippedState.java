package com.ecommerce.order_service.state;

import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.domain.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;

public class ShippedState implements OrderState {
    @Override
    public void processPayment(Order order) {
        throw new InvalidOrderStateException("Order already shipped.");
    }

    @Override
    public void shipOrder(Order order) {
        throw new InvalidOrderStateException("Order already shipped.");
    }

    @Override
    public void cancelOrder(Order order) {
        throw new InvalidOrderStateException("Cannot cancel an order that has already been shipped.");
    }

    @Override
    public void failOrder(Order order) {
        throw new InvalidOrderStateException("Cannot mark a shipped order as failed.");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.SHIPPED;
    }
}
