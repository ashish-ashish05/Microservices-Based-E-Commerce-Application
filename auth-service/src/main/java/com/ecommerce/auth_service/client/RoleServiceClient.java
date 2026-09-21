package com.ecommerce.auth_service.client;

import com.ecommerce.auth_service.dto.response.RoleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "role-service", url = "${role-service.url}")
public interface RoleServiceClient {
    @GetMapping("/roles/{roleId}")
    RoleResponse getRoleById(@PathVariable("roleId") Long roleId);
}
