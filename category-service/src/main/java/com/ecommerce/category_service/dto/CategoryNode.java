package com.ecommerce.category_service.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

// Composite Pattern – Category Tree
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryNode {
    private UUID id;
    private String name;
    private List<CategoryNode> subcategories;
}
