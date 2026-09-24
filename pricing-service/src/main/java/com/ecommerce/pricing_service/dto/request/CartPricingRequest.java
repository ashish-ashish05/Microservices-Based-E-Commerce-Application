package com.ecommerce.pricing_service.dto.request;

import lombok.*;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPricingRequest {
    @NotNull
    @NotEmpty
    private List<CartItemDTO> items;
    private String couponCode;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDTO {
        @NotNull
        private UUID productId;
        @NotNull
        private Integer quantity;
    }
}
