package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.InventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.service.InventoryService;
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

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void createOrUpdate_ShouldReturnOk() throws Exception {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(UUID.randomUUID());
        request.setQuantity(10);

        InventoryResponse response = InventoryResponse.builder()
                .productId(request.getProductId())
                .quantity(10)
                .inStock(true)
                .build();

        when(inventoryService.createOrUpdate(any())).thenReturn(response);

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": \"" + request.getProductId() + "\", \"quantity\": 10}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void createOrUpdate_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": null, \"quantity\": -1}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
