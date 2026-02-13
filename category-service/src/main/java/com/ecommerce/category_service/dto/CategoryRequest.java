package com.ecommerce.category_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CategoryRequest {
    @NotBlank
    private String name;
    private UUID parentCategoryId;
}
