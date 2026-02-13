package com.ecommerce.category_service.service;

import com.ecommerce.category_service.dto.CategoryNode;
import com.ecommerce.category_service.dto.CategoryRequest;
import com.ecommerce.category_service.dto.CategoryResponse;
import com.ecommerce.category_service.entity.Category;
import com.ecommerce.category_service.exception.CategoryNotFoundException;
import com.ecommerce.category_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;


    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        Category category = Category.builder()
                .name(categoryRequest.getName())
                .parentCategoryId(categoryRequest.getParentCategoryId())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponse getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(()-> new CategoryNotFoundException(id));
        return mapToResponse(category);
    }

    @Override
    public List<CategoryNode> getCategoryTree() {
        return buildCategoryTree(null);
    }

    private List<CategoryNode> buildCategoryTree(UUID parentId){
        List<Category> categories = categoryRepository.findByParentCategoryId(parentId);

        return categories.stream().map(category -> CategoryNode.builder()
                .id(category.getId())
                .name(category.getName())
                .subcategories(buildCategoryTree(category.getId()))
                .build())
                .toList();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
