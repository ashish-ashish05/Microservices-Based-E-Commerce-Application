package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.InventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;

import java.util.UUID;

public interface InventoryService {
    InventoryResponse createOrUpdate(InventoryRequest inventoryRequest);
    InventoryResponse getInventory(UUID productId);
    void reserveStock(UUID productId, int quantity);
    void releaseStock(UUID productId, int quantity);
}
