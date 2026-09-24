package com.ecommerce.coupon_service.strategy;

import com.ecommerce.coupon_service.strategy.impl.FixedDiscountStrategy;
import com.ecommerce.coupon_service.strategy.impl.PercentageDiscountStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscountStrategyTest {

    private final FixedDiscountStrategy fixedStrategy = new FixedDiscountStrategy();
    private final PercentageDiscountStrategy percentageStrategy = new PercentageDiscountStrategy();

    @Test
    void testFixedDiscount() {
        BigDecimal orderValue = new BigDecimal("100.00");
        BigDecimal discountValue = new BigDecimal("20.00");
        assertEquals(new BigDecimal("20.00"), fixedStrategy.calculateDiscount(orderValue, discountValue));

        BigDecimal smallOrderValue = new BigDecimal("10.00");
        assertEquals(new BigDecimal("10.00"), fixedStrategy.calculateDiscount(smallOrderValue, discountValue));
    }

    @Test
    void testPercentageDiscount() {
        BigDecimal orderValue = new BigDecimal("100.00");
        BigDecimal discountValue = new BigDecimal("20.00"); // 20%
        assertEquals(new BigDecimal("20.00"), percentageStrategy.calculateDiscount(orderValue, discountValue));

        BigDecimal orderValue2 = new BigDecimal("150.00");
        assertEquals(new BigDecimal("30.00"), percentageStrategy.calculateDiscount(orderValue2, discountValue));
    }
}
