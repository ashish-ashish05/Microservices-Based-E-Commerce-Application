package com.ecommerce.category_service.service;

import com.ecommerce.category_service.dto.CategoryNode;
import com.ecommerce.category_service.dto.CategoryRequest;
import com.ecommerce.category_service.dto.CategoryResponse;
import com.ecommerce.category_service.entity.Category;
import com.ecommerce.category_service.exception.CategoryNotFoundException;
import com.ecommerce.category_service.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category sampleCategory;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        sampleCategory = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();
    }

    @Test
    void createCategory_ShouldSaveCategory() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.save(any())).thenReturn(sampleCategory);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(sampleCategory.getName(), response.getName());
        verify(categoryRepository).save(any());
    }

    @Test
    void getCategory_ShouldReturnCategory() {
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(sampleCategory));

        CategoryResponse response = categoryService.getCategory(categoryId);

        assertNotNull(response);
        assertEquals("Electronics", response.getName());
    }

    @Test
    void getCategory_ShouldThrowException_WhenNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategory(categoryId));
    }

    @Test
    void getCategoryTree_ShouldReturnTree() {
        Category child = Category.builder().id(UUID.randomUUID()).name("Laptops").parentCategoryId(categoryId).build();

        when(categoryRepository.findByParentCategoryId(null)).thenReturn(List.of(sampleCategory));
        when(categoryRepository.findByParentCategoryId(categoryId)).thenReturn(List.of(child));
        when(categoryRepository.findByParentCategoryId(child.getId())).thenReturn(Collections.emptyList());

        List<CategoryNode> tree = categoryService.getCategoryTree();

        assertEquals(1, tree.size());
        assertEquals("Electronics", tree.get(0).getName());
        assertEquals(1, tree.get(0).getSubcategories().size());
        assertEquals("Laptops", tree.get(0).getSubcategories().get(0).getName());
    }
}
