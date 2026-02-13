package com.ecommerce.product_service.service;

import com.ecommerce.product_service.client.CategoryClient;
import com.ecommerce.product_service.client.InventoryClient;
import com.ecommerce.product_service.dto.*;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.factory.ProductFactory;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryClient categoryClient;
    private final InventoryClient inventoryClient;


    @Override
    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = ProductFactory.createProduct(productRequest);
        product = productRepository.save(product);
        try {
            inventoryClient.createOrUpdate(
                    new InventoryRequest(product.getId(), 0)
            );
        } catch (Exception ex) {
            System.out.println("Inventory creation failed: " + ex.getMessage());
        }

        return mapToProductResponse(product);
    }

    @Override
    public ProductResponse getProduct(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ProductNotFoundException(productId));
        return mapToProductResponse(product);

    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    private ProductResponse mapToProductResponse(Product product) {
        CategoryResponse categoryResponse = categoryClient.getCategory(product.getCategoryId());
        InventoryResponse inventoryResponse = inventoryClient.getInventory(product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryName(categoryResponse.getName())
                .inStock(inventoryResponse.getQuantity()>0)
                .build();
    }
}
