package com.ecommerce.category_service.controller;

import com.ecommerce.category_service.dto.CategoryRequest;
import com.ecommerce.category_service.dto.CategoryResponse;
import com.ecommerce.category_service.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void createCategory_ShouldReturnOk() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");

        CategoryResponse response = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .build();

        when(categoryService.createCategory(any())).thenReturn(response);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Electronics\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void createCategory_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
