package com.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@FeignClient(name = "inventory-service", url = "${services.inventory.url}")
public interface InventoryClient {
    @PutMapping("/inventory/{productId}/reserve")
    void reserveInventory(
        @PathVariable("productId") UUID productId,
        @RequestParam("quantity") Integer quantity
    );

    @PutMapping("/inventory/{productId}/release")
    void releaseInventory(
        @PathVariable("productId") UUID productId,
        @RequestParam("quantity") Integer quantity
    );
}
