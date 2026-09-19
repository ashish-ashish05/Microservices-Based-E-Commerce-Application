package com.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String email;
}
