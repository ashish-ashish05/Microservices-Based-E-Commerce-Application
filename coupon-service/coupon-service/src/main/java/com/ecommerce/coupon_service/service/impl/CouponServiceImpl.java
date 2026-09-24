package com.ecommerce.coupon_service.service.impl;

import com.ecommerce.coupon_service.dto.request.CouponRequest;
import com.ecommerce.coupon_service.dto.request.CouponValidationRequest;
import com.ecommerce.coupon_service.dto.response.CouponResponse;
import com.ecommerce.coupon_service.dto.response.CouponValidationResponse;
import com.ecommerce.coupon_service.entity.Coupon;
import com.ecommerce.coupon_service.exception.CouponAlreadyExistsException;
import com.ecommerce.coupon_service.exception.CouponNotFoundException;
import com.ecommerce.coupon_service.repository.CouponRepository;
import com.ecommerce.coupon_service.service.CouponService;
import com.ecommerce.coupon_service.strategy.DiscountStrategy;
import com.ecommerce.coupon_service.strategy.DiscountStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final DiscountStrategyFactory strategyFactory;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new CouponAlreadyExistsException("Coupon code already exists: " + request.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountValue(request.getDiscountValue())
                .discountType(request.getDiscountType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .minOrderValue(request.getMinOrderValue())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        return mapToResponse(savedCoupon);
    }

    @Override
    public CouponResponse getCouponDetails(String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found: " + code));
        return mapToResponse(coupon);
    }

    @Override
    public CouponValidationResponse validateCoupon(CouponValidationRequest request) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCode())
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found: " + request.getCode()));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            return CouponValidationResponse.builder()
                    .isValid(false)
                    .message("Coupon is not active or has expired")
                    .build();
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidationResponse.builder()
                    .isValid(false)
                    .message("Coupon usage limit has been reached")
                    .build();
        }

        if (coupon.getMinOrderValue() != null && request.getOrderValue().compareTo(coupon.getMinOrderValue()) < 0) {
            return CouponValidationResponse.builder()
                    .isValid(false)
                    .message("Minimum order value not met for this coupon")
                    .build();
        }

        DiscountStrategy strategy = strategyFactory.getStrategy(coupon.getDiscountType());
        var discountAmount = strategy.calculateDiscount(request.getOrderValue(), coupon.getDiscountValue());

        return CouponValidationResponse.builder()
                .isValid(true)
                .discountValue(discountAmount)
                .discountType(coupon.getDiscountType())
                .build();
    }

    @Override
    @Transactional
    public void useCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found: " + code));
        
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountValue(coupon.getDiscountValue())
                .discountType(coupon.getDiscountType())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .minOrderValue(coupon.getMinOrderValue())
                .build();
    }
}
