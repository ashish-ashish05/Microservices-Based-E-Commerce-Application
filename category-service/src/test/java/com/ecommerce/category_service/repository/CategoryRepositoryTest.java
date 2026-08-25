package com.ecommerce.category_service.repository;

import com.ecommerce.category_service.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void saveAndFindCategory_ShouldWork() {
        Category category = Category.builder()
                .name("Books")
                .build();

        Category savedCategory = categoryRepository.save(category);

        assertTrue(savedCategory.getId() != null);
        Category foundCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();
        assertEquals("Books", foundCategory.getName());
    }
}
