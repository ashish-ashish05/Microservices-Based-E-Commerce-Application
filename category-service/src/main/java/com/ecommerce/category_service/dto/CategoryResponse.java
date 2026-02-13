package com.ecommerce.category_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
}
