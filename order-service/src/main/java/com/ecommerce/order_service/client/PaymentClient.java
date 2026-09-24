package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.PaymentRequest;
import com.ecommerce.order_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentClient {
    @PostMapping("/payments/process")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}
