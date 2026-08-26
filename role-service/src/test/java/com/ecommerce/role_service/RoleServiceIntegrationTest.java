package com.ecommerce.role_service;

import com.ecommerce.role_service.dto.RoleRequest;
import com.ecommerce.role_service.dto.RoleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoleServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testFullRoleLifecycle() {
        // 1. Create Role
        RoleRequest createRequest = RoleRequest.builder()
                .name("ROLE_MANAGER")
                .description("Managerial Role")
                .build();

        ResponseEntity<RoleResponse> createResponse = restTemplate.postForEntity("/api/roles", createRequest, RoleResponse.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        UUID roleId = createResponse.getBody().getId();

        // 2. Get Role
        ResponseEntity<RoleResponse> getResponse = restTemplate.getForEntity("/api/roles/" + roleId, RoleResponse.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("ROLE_MANAGER", getResponse.getBody().getName());

        // 3. Update Role
        RoleRequest updateRequest = RoleRequest.builder()
                .name("ROLE_SUPERVISOR")
                .description("Updated Description")
                .build();

        HttpEntity<RoleRequest> httpEntity = new HttpEntity<>(updateRequest);
        ResponseEntity<RoleResponse> updateResponse = restTemplate.exchange(
                "/api/roles/" + roleId,
                HttpMethod.PUT,
                httpEntity,
                RoleResponse.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("ROLE_SUPERVISOR", updateResponse.getBody().getName());

        // 4. List Roles
        ResponseEntity<RoleResponse[]> listResponse = restTemplate.getForEntity("/api/roles", RoleResponse[].class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertTrue(listResponse.getBody().length > 0);

        // 5. Delete Role
        restTemplate.delete("/api/roles/" + roleId);
        ResponseEntity<RoleResponse> finalGetResponse = restTemplate.getForEntity("/api/roles/" + roleId, RoleResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, finalGetResponse.getStatusCode());
    }

    @Test
    void testCreateDuplicateRole_ShouldReturnConflict() {
        RoleRequest request = RoleRequest.builder()
                .name("ROLE_UNIQUE")
                .build();

        restTemplate.postForEntity("/api/roles", request, RoleResponse.class);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/roles", request, String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testCreateInvalidRole_ShouldReturnBadRequest() {
        RoleRequest request = RoleRequest.builder()
                .name("INVALID_NAME") // Doesn't start with ROLE_
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/roles", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
