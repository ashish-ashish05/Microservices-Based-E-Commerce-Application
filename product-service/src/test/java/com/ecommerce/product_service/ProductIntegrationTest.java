package com.ecommerce.product_service;

import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.ecommerce.product_service.client.CategoryClient;
import com.ecommerce.product_service.client.InventoryClient;
import com.ecommerce.product_service.dto.CategoryResponse;
import com.ecommerce.product_service.dto.InventoryResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private CategoryClient categoryClient;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void testCreateAndGetProductFlow() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = new ProductRequest();
        request.setName("Integration Product");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setCategoryId(categoryId);

        when(categoryClient.getCategory(any())).thenReturn(CategoryResponse.builder().id(categoryId).name("Integration Category").build());
        when(inventoryClient.getInventory(any())).thenReturn(InventoryResponse.builder().productId(UUID.randomUUID()).quantity(5).inStock(true).build());

        ProductResponse created = productService.createProduct(request);
        assertNotNull(created);
        assertEquals("Integration Product", created.getName());

        ProductResponse retrieved = productService.getProduct(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }
}
