package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.entity.Inventory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void saveAndFindInventory_ShouldWork() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .quantity(50)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        assertTrue(savedInventory.getId() != null);
        Inventory foundInventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertEquals(50, foundInventory.getQuantity());
    }
}
