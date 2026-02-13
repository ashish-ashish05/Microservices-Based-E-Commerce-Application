package com.ecommerce.product_service.client;

import com.ecommerce.product_service.dto.CategoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "category-service", url = "${category.service.url}")
public interface CategoryClient {
    @GetMapping("/categories/{id}")
    CategoryResponse getCategory(@PathVariable UUID id);
}
