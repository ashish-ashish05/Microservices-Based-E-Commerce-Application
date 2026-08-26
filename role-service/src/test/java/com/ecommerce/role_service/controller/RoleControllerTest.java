package com.ecommerce.role_service.controller;

import com.ecommerce.role_service.dto.RoleRequest;
import com.ecommerce.role_service.dto.RoleResponse;
import com.ecommerce.role_service.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @Test
    void getAllRoles_ShouldReturnList() throws Exception {
        List<RoleResponse> roles = List.of(
                RoleResponse.builder().id(UUID.randomUUID()).name("ROLE_USER").build(),
                RoleResponse.builder().id(UUID.randomUUID()).name("ROLE_ADMIN").build()
        );

        when(roleService.getAllRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("ROLE_USER"));
    }

    @Test
    void getRole_ShouldReturnRole_WhenIdExists() throws Exception {
        UUID id = UUID.randomUUID();
        RoleResponse response = RoleResponse.builder()
                .id(id)
                .name("ROLE_USER")
                .build();

        when(roleService.getRole(id)).thenReturn(response);

        mockMvc.perform(get("/api/roles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("ROLE_USER"));
    }

    @Test
    void createRole_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_MANAGER")
                .description("Manager role")
                .build();
        RoleResponse response = RoleResponse.builder()
                .id(UUID.randomUUID())
                .name("ROLE_MANAGER")
                .description("Manager role")
                .build();

        when(roleService.createRole(any(RoleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ROLE_MANAGER\",\"description\":\"Manager role\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ROLE_MANAGER"));
    }

    @Test
    void createRole_ShouldReturnBadRequest_WhenNameIsInvalid() throws Exception {
        // Name does not start with ROLE_
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USER\",\"description\":\"Invalid role\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_ShouldReturnBadRequest_WhenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Missing name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_UPDATED")
                .build();
        RoleResponse response = RoleResponse.builder()
                .id(id)
                .name("ROLE_UPDATED")
                .build();

        when(roleService.updateRole(eq(id), any(RoleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/roles/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ROLE_UPDATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_UPDATED"));
    }

    @Test
    void deleteRole_ShouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/roles/" + id))
                .andExpect(status().isNoContent());

        verify(roleService).deleteRole(id);
    }
}
