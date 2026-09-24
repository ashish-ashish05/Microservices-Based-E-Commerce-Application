package com.ecommerce.order_service.command;

import com.ecommerce.order_service.client.ShippingClient;
import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.dto.ShippingRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateShippingCommand implements OrderCommand {
    private final ShippingClient shippingClient;
    private final Order order;
    private final String address;

    @Override
    public void execute() {
        ShippingRequest request = ShippingRequest.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .address(address)
                .build();
        shippingClient.createShipment(request);
    }

    @Override
    public void undo() {
        // Implement shipment cancellation if needed
    }
}
