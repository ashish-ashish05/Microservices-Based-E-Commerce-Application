package com.ecommerce.coupon_service.service;

import com.ecommerce.coupon_service.dto.request.CouponRequest;
import com.ecommerce.coupon_service.dto.request.CouponValidationRequest;
import com.ecommerce.coupon_service.dto.response.CouponResponse;
import com.ecommerce.coupon_service.dto.response.CouponValidationResponse;

import java.util.UUID;

public interface CouponService {
    CouponResponse createCoupon(CouponRequest request);
    CouponResponse getCouponDetails(String code);
    CouponValidationResponse validateCoupon(CouponValidationRequest request);
    void useCoupon(String code);
}
