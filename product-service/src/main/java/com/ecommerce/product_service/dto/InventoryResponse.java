package com.ecommerce.product_service.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private UUID productId;
    private int quantity;
    private boolean inStock;
}
