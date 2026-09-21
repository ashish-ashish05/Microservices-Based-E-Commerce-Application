package com.ecommerce.auth_service.client;

import com.ecommerce.auth_service.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {
    @GetMapping("/users/{username}")
    UserResponse getUserByUsername(@PathVariable("username") String username);
}
