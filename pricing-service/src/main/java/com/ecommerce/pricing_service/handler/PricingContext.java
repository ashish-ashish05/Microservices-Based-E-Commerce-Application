package com.ecommerce.pricing_service.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PricingContext {
    private final UUID productId;
    private final String couponCode;
}
