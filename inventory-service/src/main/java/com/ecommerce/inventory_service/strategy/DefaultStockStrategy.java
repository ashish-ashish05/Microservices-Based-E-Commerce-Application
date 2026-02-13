package com.ecommerce.inventory_service.strategy;

import org.springframework.stereotype.Component;

@Component
public class DefaultStockStrategy implements StockStrategy {

    @Override
    public boolean isAvailable(int quantity) {
        return quantity>0;
    }
}
