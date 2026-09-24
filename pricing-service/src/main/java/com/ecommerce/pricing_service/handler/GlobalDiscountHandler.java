package com.ecommerce.pricing_service.handler;

import com.ecommerce.pricing_service.domain.PricingRule;
import com.ecommerce.pricing_service.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GlobalDiscountHandler extends DiscountHandler {
    private final PricingRuleRepository repository;

    @Override
    public BigDecimal handle(PricingContext context, BigDecimal currentPrice) {
        List<PricingRule> globalRules = repository.findActiveRules(LocalDateTime.now())
                .stream()
                .filter(rule -> rule.getProductId() == null)
                .toList();

        BigDecimal finalPrice = currentPrice;
        if (!globalRules.isEmpty()) {
            // Apply the best (maximum) discount
            double maxDiscount = globalRules.stream()
                    .mapToDouble(PricingRule::getDiscountPercent)
                    .max()
                    .orElse(0.0);

            BigDecimal discountMultiplier = BigDecimal.valueOf(1.0 - (maxDiscount / 100.0));
            finalPrice = currentPrice.multiply(discountMultiplier);
        }

        return next(context, finalPrice);
    }
}
