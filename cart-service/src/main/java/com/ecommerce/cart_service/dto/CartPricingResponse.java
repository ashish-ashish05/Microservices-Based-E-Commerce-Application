package com.ecommerce.cart_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPricingResponse {
    private BigDecimal subTotal;
    private BigDecimal discountTotal;
    private BigDecimal finalTotal;
    private String currency;
    private List<PricingBreakdown> breakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingBreakdown {
        private UUID productId;
        private BigDecimal itemTotal;
        private BigDecimal discount;
    }
}
