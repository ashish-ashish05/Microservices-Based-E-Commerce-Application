package com.ecommerce.pricing_service.strategy;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculate(BigDecimal basePrice, int quantity);
}
