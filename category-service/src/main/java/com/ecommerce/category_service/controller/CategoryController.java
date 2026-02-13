package com.ecommerce.category_service.controller;

import com.ecommerce.category_service.dto.CategoryNode;
import com.ecommerce.category_service.dto.CategoryRequest;
import com.ecommerce.category_service.dto.CategoryResponse;
import com.ecommerce.category_service.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);
        return ResponseEntity.ok().body(categoryResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable UUID id) {
        CategoryResponse categoryResponse = categoryService.getCategory(id);
        return ResponseEntity.ok().body(categoryResponse);
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryNode>> getCategoryTree(){
        return ResponseEntity.ok().body(categoryService.getCategoryTree());
    }


}
