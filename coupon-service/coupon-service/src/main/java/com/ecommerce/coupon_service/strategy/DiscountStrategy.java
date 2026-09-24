package com.ecommerce.coupon_service.strategy;

import java.math.BigDecimal;

public interface DiscountStrategy {
    BigDecimal calculateDiscount(BigDecimal orderValue, BigDecimal discountValue);
}
