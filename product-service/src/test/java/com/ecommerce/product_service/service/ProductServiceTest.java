package com.ecommerce.product_service.service;

import com.ecommerce.product_service.client.CategoryClient;
import com.ecommerce.product_service.client.InventoryClient;
import com.ecommerce.product_service.dto.*;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryClient categoryClient;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private UUID productId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        sampleProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.TEN)
                .categoryId(categoryId)
                .build();
    }

    @Test
    void createProduct_ShouldSaveAndCreateInventory() {
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(BigDecimal.TEN);
        request.setCategoryId(categoryId);

        when(productRepository.save(any())).thenReturn(sampleProduct);
        when(categoryClient.getCategory(categoryId)).thenReturn(CategoryResponse.builder().id(categoryId).name("Test Category").build());
        when(inventoryClient.getInventory(productId)).thenReturn(InventoryResponse.builder().productId(productId).quantity(10).inStock(true).build());

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(sampleProduct.getName(), response.getName());
        verify(productRepository).save(any());
        verify(inventoryClient).createOrUpdate(any());
    }

    @Test
    void getProduct_ShouldReturnProductResponse() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(categoryClient.getCategory(categoryId)).thenReturn(CategoryResponse.builder().id(categoryId).name("Test Category").build());
        when(inventoryClient.getInventory(productId)).thenReturn(InventoryResponse.builder().productId(productId).quantity(10).inStock(true).build());

        ProductResponse response = productService.getProduct(productId);

        assertNotNull(response);
        assertEquals("Test Category", response.getCategoryName());
        assertTrue(response.isInStock());
    }

    @Test
    void getProduct_ShouldThrowException_WhenProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProduct(productId));
    }
}
