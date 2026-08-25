package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void saveAndFindProduct_ShouldWork() {
        UUID categoryId = UUID.randomUUID();
        Product product = Product.builder()
                .name("Repo Product")
                .description("Repo Desc")
                .price(BigDecimal.TEN)
                .categoryId(categoryId)
                .build();

        Product savedProduct = productRepository.save(product);

        assertTrue(savedProduct.getId() != null);
        Product foundProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals("Repo Product", foundProduct.getName());
    }
}
