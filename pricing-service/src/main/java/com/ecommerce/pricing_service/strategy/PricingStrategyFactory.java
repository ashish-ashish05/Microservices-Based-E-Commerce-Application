package com.ecommerce.pricing_service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PricingStrategyFactory {
    private final Map<String, PricingStrategy> strategies = new ConcurrentHashMap<>();

    public PricingStrategyFactory(RegularPricingStrategy regular, BulkPricingStrategy bulk) {
        strategies.put("REGULAR", regular);
        strategies.put("BULK", bulk);
    }

    public PricingStrategy getStrategy(int quantity) {
        if (quantity >= 10) {
            return strategies.get("BULK");
        }
        return strategies.get("REGULAR");
    }
}
