package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${services.cart.url}")
public interface CartClient {
    @GetMapping("/carts/{userId}")
    CartResponse getCart(@PathVariable("userId") String userId);
}
