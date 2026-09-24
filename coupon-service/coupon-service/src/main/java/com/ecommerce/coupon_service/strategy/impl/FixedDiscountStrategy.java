package com.ecommerce.coupon_service.strategy.impl;

import com.ecommerce.coupon_service.strategy.DiscountStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FixedDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(BigDecimal orderValue, BigDecimal discountValue) {
        // Discount cannot exceed order value
        return orderValue.compareTo(discountValue) < 0 ? orderValue : discountValue;
    }
}
