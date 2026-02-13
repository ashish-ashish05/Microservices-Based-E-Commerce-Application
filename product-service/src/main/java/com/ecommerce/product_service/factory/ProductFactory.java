package com.ecommerce.product_service.factory;

import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.entity.Product;

public class ProductFactory {
    private ProductFactory() {}

    public static Product createProduct(ProductRequest productRequest) {
        return Product
                .builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .categoryId(productRequest.getCategoryId())
                .build();
    }
}
