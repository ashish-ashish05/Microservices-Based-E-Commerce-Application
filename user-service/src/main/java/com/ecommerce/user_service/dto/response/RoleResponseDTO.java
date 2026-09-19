package com.ecommerce.user_service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponseDTO {
    private UUID id;
    private String name;
    private String description;
}
