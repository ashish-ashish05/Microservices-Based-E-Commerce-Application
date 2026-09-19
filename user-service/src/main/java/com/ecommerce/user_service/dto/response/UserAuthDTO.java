package com.ecommerce.user_service.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthDTO {

    private UUID id;
    private String passwordHash;
    private UUID roleId;
}
