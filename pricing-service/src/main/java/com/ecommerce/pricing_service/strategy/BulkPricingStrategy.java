package com.ecommerce.pricing_service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class BulkPricingStrategy implements PricingStrategy {
    private static final int BULK_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT_RATE = new BigDecimal("0.90"); // 10% off for bulk

    @Override
    public BigDecimal calculate(BigDecimal basePrice, int quantity) {
        BigDecimal total = basePrice.multiply(BigDecimal.valueOf(quantity));
        if (quantity >= BULK_THRESHOLD) {
            return total.multiply(BULK_DISCOUNT_RATE);
        }
        return total;
    }
}
