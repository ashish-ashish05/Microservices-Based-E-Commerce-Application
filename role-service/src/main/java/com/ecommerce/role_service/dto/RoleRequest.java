package com.ecommerce.role_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^ROLE_.*", message = "Role name must start with 'ROLE_'")
    private String name;

    private String description;
}
