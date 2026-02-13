package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.dto.InventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.entity.Inventory;
import com.ecommerce.inventory_service.exception.InventoryNotFoundException;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.strategy.StockStrategy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService{

    private final InventoryRepository inventoryRepository;
    private final StockStrategy stockStrategy;

    @Override
    public InventoryResponse createOrUpdate(InventoryRequest inventoryRequest) {
        Inventory inventory = inventoryRepository
                .findByProductId(inventoryRequest.getProductId())
                .orElse(Inventory.builder()
                        .productId(inventoryRequest.getProductId())
                        .quantity(0)
                        .build());

        inventory.setQuantity(inventoryRequest.getQuantity());
        inventoryRepository.save(inventory);

        return mapToResponse(inventory);

    }

    @Override
    public InventoryResponse getInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId));
        return mapToResponse(inventory);
    }

    @Override
    public void reserveStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId));
        if(inventory.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient Stock");
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void releaseStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(()-> new InventoryNotFoundException(productId));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .quantity(inventory.getQuantity())
                .inStock(stockStrategy.isAvailable(inventory.getQuantity()))
                .build();
    }
}
