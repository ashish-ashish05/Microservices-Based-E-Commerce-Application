package com.ecommerce.order_service.state;

import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.domain.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;

public class TerminalState implements OrderState {
    private final OrderStatus status;

    public TerminalState(OrderStatus status) {
        this.status = status;
    }

    @Override
    public void processPayment(Order order) {
        throw new InvalidOrderStateException("Order is in a terminal state: " + status);
    }

    @Override
    public void shipOrder(Order order) {
        throw new InvalidOrderStateException("Order is in a terminal state: " + status);
    }

    @Override
    public void cancelOrder(Order order) {
        throw new InvalidOrderStateException("Order is in a terminal state: " + status);
    }

    @Override
    public void failOrder(Order order) {
        throw new InvalidOrderStateException("Order is in a terminal state: " + status);
    }

    @Override
    public OrderStatus getStatus() {
        return status;
    }
}
