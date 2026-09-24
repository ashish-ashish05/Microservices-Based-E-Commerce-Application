package com.ecommerce.order_service.state;

import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.domain.OrderStatus;

public class OrderStateFactory {
    public static OrderState getState(OrderStatus status) {
        return switch (status) {
            case PENDING -> new PendingState();
            case PAID -> new PaidState();
            case SHIPPED -> new ShippedState();
            case CANCELLED, FAILED -> new TerminalState(status);
        };
    }
}
