package com.ecommerce.user_service.client;

import com.ecommerce.user_service.dto.response.RoleResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "role-service", url = "${services.role-service.url}")
public interface RoleServiceClient {

    @GetMapping("/api/roles/{roleId}")
    RoleResponseDTO getRoleById(@PathVariable("roleId") UUID roleId);
}
