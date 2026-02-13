package com.ecommerce.inventory_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class InventoryResponse {
    private UUID productId;
    private int quantity;
    private boolean inStock;
}
