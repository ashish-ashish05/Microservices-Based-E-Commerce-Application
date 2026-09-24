package com.ecommerce.coupon_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    private String userId;

    @NotNull(message = "Order value is required")
    @Positive(message = "Order value must be positive")
    private BigDecimal orderValue;
}
