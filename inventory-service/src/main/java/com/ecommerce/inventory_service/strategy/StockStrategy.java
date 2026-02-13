package com.ecommerce.inventory_service.strategy;


public interface StockStrategy {
    boolean isAvailable(int quantity);
}
