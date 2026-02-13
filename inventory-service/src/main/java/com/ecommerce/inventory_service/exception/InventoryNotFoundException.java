package com.ecommerce.inventory_service.exception;

import java.util.UUID;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(UUID productId) {
        super("Inventory with id " + productId + " not found");
    }
}
