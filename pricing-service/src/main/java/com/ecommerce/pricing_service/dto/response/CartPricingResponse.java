package com.ecommerce.pricing_service.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPricingResponse {
    private BigDecimal subTotal;
    private BigDecimal discountTotal;
    private BigDecimal finalTotal;
    private String currency;
    private List<CartItemPricingBreakdown> breakdown;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemPricingBreakdown {
        private java.util.UUID productId;
        private BigDecimal itemTotal;
        private BigDecimal discount;
    }
}
