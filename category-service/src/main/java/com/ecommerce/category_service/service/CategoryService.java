package com.ecommerce.category_service.service;

import com.ecommerce.category_service.dto.CategoryNode;
import com.ecommerce.category_service.dto.CategoryRequest;
import com.ecommerce.category_service.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse getCategory(UUID id);
    List<CategoryNode> getCategoryTree();
}
