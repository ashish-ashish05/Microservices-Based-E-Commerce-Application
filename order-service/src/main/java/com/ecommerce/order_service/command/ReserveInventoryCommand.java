package com.ecommerce.order_service.command;

import com.ecommerce.order_service.client.InventoryClient;
import com.ecommerce.order_service.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class ReserveInventoryCommand implements OrderCommand {
    private final InventoryClient inventoryClient;
    private final List<OrderItem> items;

    @Override
    public void execute() {
        for (OrderItem item : items) {
            inventoryClient.reserveInventory(item.getProductId(), item.getQuantity());
        }
    }

    @Override
    public void undo() {
        for (OrderItem item : items) {
            inventoryClient.releaseInventory(item.getProductId(), item.getQuantity());
        }
    }
}
