package com.ecommerce.cart_service.client;

import com.ecommerce.cart_service.dto.CartPricingRequest;
import com.ecommerce.cart_service.dto.CartPricingResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pricing-service", url = "${pricing-service.url}")
public interface PricingClient {

    @PostMapping("/pricing/calculate-cart")
    @CircuitBreaker(name = "pricingService", fallbackMethod = "calculateCartFallback")
    CartPricingResponse calculateCart(@RequestBody CartPricingRequest request);

    default CartPricingResponse calculateCartFallback(CartPricingRequest request, Throwable t) {
        return CartPricingResponse.builder()
                .finalTotal(java.math.BigDecimal.ZERO)
                .currency("USD")
                .subTotal(java.math.BigDecimal.ZERO)
                .discountTotal(java.math.BigDecimal.ZERO)
                .breakdown(java.util.Collections.emptyList())
                .build();
    }
}
