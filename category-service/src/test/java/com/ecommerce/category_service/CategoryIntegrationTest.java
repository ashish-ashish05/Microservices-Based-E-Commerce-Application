package com.ecommerce.category_service;

import com.ecommerce.category_service.dto.CategoryNode;
import com.ecommerce.category_service.dto.CategoryRequest;
import com.ecommerce.category_service.dto.CategoryResponse;
import com.ecommerce.category_service.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class CategoryIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void testCreateAndGetCategoryFlow() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Home & Garden");

        CategoryResponse created = categoryService.createCategory(request);
        assertNotNull(created);
        assertEquals("Home & Garden", created.getName());

        CategoryResponse retrieved = categoryService.getCategory(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void testCategoryTreeFlow() {
        // Create root
        CategoryRequest rootReq = new CategoryRequest();
        rootReq.setName("Root");
        CategoryResponse root = categoryService.createCategory(rootReq);

        // Create child
        CategoryRequest childReq = new CategoryRequest();
        childReq.setName("Child");
        childReq.setParentCategoryId(root.getId());
        categoryService.createCategory(childReq);

        List<CategoryNode> tree = categoryService.getCategoryTree();
        assertFalse(tree.isEmpty());
        assertTrue(tree.stream().anyMatch(node -> node.getName().equals("Root") && !node.getSubcategories().isEmpty()));
    }
}
