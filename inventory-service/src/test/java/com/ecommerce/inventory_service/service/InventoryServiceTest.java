package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.InventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.exception.InventoryNotFoundException;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.strategy.StockStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockStrategy stockStrategy;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory sampleInventory;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        sampleInventory = Inventory.builder()
                .productId(productId)
                .quantity(10)
                .build();
    }

    @Test
    void createOrUpdate_ShouldSaveInventory() {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(productId);
        request.setQuantity(20);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(stockStrategy.isAvailable(any(Integer.class))).thenReturn(true);

        InventoryResponse response = inventoryService.createOrUpdate(request);

        assertNotNull(response);
        assertEquals(20, response.getQuantity());
        verify(inventoryRepository).save(any());
    }

    @Test
    void getInventory_ShouldReturnInventoryResponse() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));
        when(stockStrategy.isAvailable(any(Integer.class))).thenReturn(true);

        InventoryResponse response = inventoryService.getInventory(productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
    }

    @Test
    void getInventory_ShouldThrowException_WhenNotFound() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.getInventory(productId));
    }

    @Test
    void reserveStock_ShouldDecrementQuantity() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        inventoryService.reserveStock(productId, 5);

        assertEquals(5, sampleInventory.getQuantity());
        verify(inventoryRepository).save(sampleInventory);
    }

    @Test
    void reserveStock_ShouldThrowException_WhenInsufficientStock() {
        sampleInventory.setQuantity(2);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        assertThrows(IllegalStateException.class, () -> inventoryService.reserveStock(productId, 5));
    }

    @Test
    void releaseStock_ShouldIncrementQuantity() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(sampleInventory));

        inventoryService.releaseStock(productId, 5);

        assertEquals(15, sampleInventory.getQuantity());
        verify(inventoryRepository).save(sampleInventory);
    }
}
