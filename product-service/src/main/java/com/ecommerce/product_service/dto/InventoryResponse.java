package com.ecommerce.product_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InventoryResponse {
    private UUID productId;
    private int quantity;
}
