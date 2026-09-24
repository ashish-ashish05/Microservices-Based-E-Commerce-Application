package com.ecommerce.order_service.command;

import com.ecommerce.order_service.client.PaymentClient;
import com.ecommerce.order_service.domain.Order;
import com.ecommerce.order_service.dto.PaymentRequest;
import com.ecommerce.order_service.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;

@RequiredArgsConstructor
public class ProcessPaymentCommand implements OrderCommand {
    private final PaymentClient paymentClient;
    private final Order order;

    @Override
    public void execute() {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .paymentDetails(new HashMap<>())
                .build();

        PaymentResponse response = paymentClient.processPayment(request);
        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            throw new RuntimeException("Payment failed with status: " + response.getStatus());
        }
    }

    @Override
    public void undo() {
        // Implement refund logic if needed
    }
}
