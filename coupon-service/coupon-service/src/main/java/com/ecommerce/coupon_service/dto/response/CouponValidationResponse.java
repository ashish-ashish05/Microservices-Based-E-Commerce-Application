package com.ecommerce.coupon_service.dto.response;

import com.ecommerce.coupon_service.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {

    private boolean isValid;
    private BigDecimal discountValue;
    private DiscountType discountType;
    private String message;
}
