package com.ecommerce.coupon_service.strategy;

import com.ecommerce.coupon_service.entity.DiscountType;
import com.ecommerce.coupon_service.strategy.impl.FixedDiscountStrategy;
import com.ecommerce.coupon_service.strategy.impl.PercentageDiscountStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DiscountStrategyFactory {

    private final Map<DiscountType, DiscountStrategy> strategies = new ConcurrentHashMap<>();

    public DiscountStrategyFactory(FixedDiscountStrategy fixed, PercentageDiscountStrategy percentage) {
        strategies.put(DiscountType.FIXED, fixed);
        strategies.put(DiscountType.PERCENTAGE, percentage);
    }

    public DiscountStrategy getStrategy(DiscountType type) {
        DiscountStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported discount type: " + type);
        }
        return strategy;
    }
}
