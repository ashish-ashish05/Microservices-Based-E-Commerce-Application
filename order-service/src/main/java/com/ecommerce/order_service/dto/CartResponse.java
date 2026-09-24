package com.ecommerce.order_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private String userId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private Integer totalItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private UUID productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subTotal;
    }
}
