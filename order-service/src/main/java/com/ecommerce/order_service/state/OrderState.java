package com.ecommerce.order_service.state;

import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.domain.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;

public interface OrderState {
    void processPayment(Order order);
    void shipOrder(Order order);
    void cancelOrder(Order order);
    void failOrder(Order order);
    OrderStatus getStatus();
}
