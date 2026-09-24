package com.ecommerce.pricing_service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RegularPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculate(BigDecimal basePrice, int quantity) {
        return basePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
