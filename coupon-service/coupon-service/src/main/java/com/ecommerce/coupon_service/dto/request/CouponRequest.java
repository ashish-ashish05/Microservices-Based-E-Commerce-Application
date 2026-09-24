package com.ecommerce.coupon_service.dto.request;

import com.ecommerce.coupon_service.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @PositiveOrZero(message = "Usage limit must be zero or positive")
    private Long usageLimit;

    @PositiveOrZero(message = "Minimum order value must be zero or positive")
    private BigDecimal minOrderValue;
}
