package com.ecommerce.coupon_service.dto.response;

import com.ecommerce.coupon_service.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private UUID id;
    private String code;
    private BigDecimal discountValue;
    private DiscountType discountType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long usageLimit;
    private Long usedCount;
    private BigDecimal minOrderValue;
}
