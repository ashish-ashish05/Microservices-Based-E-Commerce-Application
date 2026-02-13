package com.ecommerce.product_service.client;

import com.ecommerce.product_service.dto.InventoryRequest;
import com.ecommerce.product_service.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "inventory-service", url = "${inventory.service.url}")
public interface InventoryClient {
    @GetMapping("/inventory/{productId}")
    InventoryResponse getInventory(@PathVariable UUID productId);

    @PostMapping("/inventory")
    InventoryResponse createOrUpdate(@RequestBody InventoryRequest request);

}
