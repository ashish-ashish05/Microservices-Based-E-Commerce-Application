package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.InventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createOrUpdate(
            @Valid @RequestBody InventoryRequest inventoryRequest
    ){
        return ResponseEntity.ok(inventoryService.createOrUpdate(inventoryRequest));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable UUID productId
    ){
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @PutMapping("{productId}/reserve")
    public ResponseEntity<Void> reserveStock(
            @PathVariable UUID productId, @RequestParam int quantity
    ){
        inventoryService.reserveStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @PutMapping("{productId}/release")
    public ResponseEntity<Void> releaseStock(
            @PathVariable UUID productId, @RequestParam int quantity
    ){
        inventoryService.releaseStock(productId, quantity);
        return ResponseEntity.ok().build();
    }
}
