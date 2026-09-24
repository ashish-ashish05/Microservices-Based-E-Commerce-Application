package com.ecommerce.pricing_service.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceResponse {
    private UUID productId;
    private BigDecimal basePrice;
    private BigDecimal finalPrice;
    private BigDecimal discountApplied;
    private String currency;
}
