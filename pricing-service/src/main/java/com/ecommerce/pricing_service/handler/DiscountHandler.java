package com.ecommerce.pricing_service.handler;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public abstract class DiscountHandler {
    protected DiscountHandler next;

    public void setNext(DiscountHandler next) {
        this.next = next;
    }

    public abstract BigDecimal handle(PricingContext context, BigDecimal currentPrice);

    protected BigDecimal next(PricingContext context, BigDecimal currentPrice) {
        if (next == null) {
            return currentPrice;
        }
        return next.handle(context, currentPrice);
    }
}
