package com.ecommerce.category_service.repository;

import com.ecommerce.category_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByParentCategoryId(UUID parentCategoryId);
}
