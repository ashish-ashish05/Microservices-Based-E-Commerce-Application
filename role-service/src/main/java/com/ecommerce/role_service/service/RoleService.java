package com.ecommerce.role_service.service;

import com.ecommerce.role_service.dto.RoleRequest;
import com.ecommerce.role_service.dto.RoleResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    RoleResponse createRole(RoleRequest request);
    RoleResponse getRole(UUID id);
    List<RoleResponse> getAllRoles();
    RoleResponse updateRole(UUID id, RoleRequest request);
    void deleteRole(UUID id);
}
