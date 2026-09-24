package com.ecommerce.coupon_service.strategy.impl;

import com.ecommerce.coupon_service.strategy.DiscountStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {
    @Override
    public BigDecimal calculateDiscount(BigDecimal orderValue, BigDecimal discountValue) {
        return orderValue.multiply(discountValue)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
