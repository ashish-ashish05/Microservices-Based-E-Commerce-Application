package com.ecommerce.inventory_service;

import com.ecommerce.inventory_service.dto.InventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void testCreateAndGetInventoryFlow() {
        UUID productId = UUID.randomUUID();
        InventoryRequest request = new InventoryRequest();
        request.setProductId(productId);
        request.setQuantity(100);

        InventoryResponse created = inventoryService.createOrUpdate(request);
        assertNotNull(created);
        assertEquals(100, created.getQuantity());

        InventoryResponse retrieved = inventoryService.getInventory(productId);
        assertNotNull(retrieved);
        assertEquals(100, retrieved.getQuantity());
    }

    @Test
    void testReserveAndReleaseFlow() {
        UUID productId = UUID.randomUUID();
        InventoryRequest request = new InventoryRequest();
        request.setProductId(productId);
        request.setQuantity(50);
        inventoryService.createOrUpdate(request);

        inventoryService.reserveStock(productId, 20);
        InventoryResponse afterReserve = inventoryService.getInventory(productId);
        assertEquals(30, afterReserve.getQuantity());

        inventoryService.releaseStock(productId, 10);
        InventoryResponse afterRelease = inventoryService.getInventory(productId);
        assertEquals(40, afterRelease.getQuantity());
    }
}
