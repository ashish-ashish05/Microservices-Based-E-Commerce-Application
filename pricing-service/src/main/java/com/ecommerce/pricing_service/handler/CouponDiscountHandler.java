package com.ecommerce.pricing_service.handler;

import com.ecommerce.pricing_service.client.CouponServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CouponDiscountHandler extends DiscountHandler {
    private final CouponServiceClient couponClient;

    @Override
    public BigDecimal handle(PricingContext context, BigDecimal currentPrice) {
        if (context.getCouponCode() == null || context.getCouponCode().isBlank()) {
            return next(context, currentPrice);
        }

        try {
            Double discountPercent = couponClient.getDiscountValue(context.getCouponCode());
            if (discountPercent != null && discountPercent > 0) {
                BigDecimal discountMultiplier = BigDecimal.valueOf(1.0 - (discountPercent / 100.0));
                return next(context, currentPrice.multiply(discountMultiplier));
            }
        } catch (Exception e) {
            // Fallback to 0 discount on error (Resilience)
        }

        return next(context, currentPrice);
    }
}
