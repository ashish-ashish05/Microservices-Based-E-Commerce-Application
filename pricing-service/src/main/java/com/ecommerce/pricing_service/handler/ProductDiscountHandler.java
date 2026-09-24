package com.ecommerce.pricing_service.handler;

import com.ecommerce.pricing_service.domain.PricingRule;
import com.ecommerce.pricing_service.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductDiscountHandler extends DiscountHandler {
    private final PricingRuleRepository repository;

    @Override
    public BigDecimal handle(PricingContext context, BigDecimal currentPrice) {
        if (context.getProductId() == null) {
            return next(context, currentPrice);
        }

        List<PricingRule> productRules = repository.findActiveRuleByProductId(context.getProductId(), LocalDateTime.now());

        BigDecimal finalPrice = currentPrice;
        if (!productRules.isEmpty()) {
            double maxDiscount = productRules.stream()
                    .mapToDouble(PricingRule::getDiscountPercent)
                    .max()
                    .orElse(0.0);

            BigDecimal discountMultiplier = BigDecimal.valueOf(1.0 - (maxDiscount / 100.0));
            finalPrice = currentPrice.multiply(discountMultiplier);
        }

        return next(context, finalPrice);
    }
}
