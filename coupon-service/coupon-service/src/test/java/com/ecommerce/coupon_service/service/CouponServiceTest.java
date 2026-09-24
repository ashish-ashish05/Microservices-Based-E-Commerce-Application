package com.ecommerce.coupon_service.service;

import com.ecommerce.coupon_service.dto.request.CouponRequest;
import com.ecommerce.coupon_service.dto.request.CouponValidationRequest;
import com.ecommerce.coupon_service.dto.response.CouponValidationResponse;
import com.ecommerce.coupon_service.entity.Coupon;
import com.ecommerce.coupon_service.entity.DiscountType;
import com.ecommerce.coupon_service.exception.CouponNotFoundException;
import com.ecommerce.coupon_service.repository.CouponRepository;
import com.ecommerce.coupon_service.service.impl.CouponServiceImpl;
import com.ecommerce.coupon_service.strategy.DiscountStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private DiscountStrategyFactory strategyFactory;
    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon validCoupon;

    @BeforeEach
    void setUp() {
        validCoupon = Coupon.builder()
                .code("SAVE20")
                .discountValue(new BigDecimal("20.00"))
                .discountType(DiscountType.PERCENTAGE)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .usageLimit(10L)
                .usedCount(0L)
                .minOrderValue(new BigDecimal("50.00"))
                .build();
    }

    @Test
    void validateCoupon_Success() {
        CouponValidationRequest request = CouponValidationRequest.builder()
                .code("SAVE20")
                .orderValue(new BigDecimal("100.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(validCoupon));
        when(strategyFactory.getStrategy(DiscountType.PERCENTAGE)).thenReturn((ov, dv) -> ov.multiply(dv).divide(new BigDecimal("100")));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertTrue(response.isValid());
        assertEquals(new BigDecimal("20.00"), response.getDiscountValue());
    }

    @Test
    void validateCoupon_Expired() {
        validCoupon.setEndDate(LocalDateTime.now().minusDays(1));
        CouponValidationRequest request = CouponValidationRequest.builder()
                .code("SAVE20")
                .orderValue(new BigDecimal("100.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(validCoupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertFalse(response.isValid());
        assertEquals("Coupon is not active or has expired", response.getMessage());
    }

    @Test
    void validateCoupon_LimitReached() {
        validCoupon.setUsedCount(10L);
        CouponValidationRequest request = CouponValidationRequest.builder()
                .code("SAVE20")
                .orderValue(new BigDecimal("100.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(validCoupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertFalse(response.isValid());
        assertEquals("Coupon usage limit has been reached", response.getMessage());
    }

    @Test
    void validateCoupon_MinOrderNotMet() {
        CouponValidationRequest request = CouponValidationRequest.builder()
                .code("SAVE20")
                .orderValue(new BigDecimal("40.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(validCoupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertFalse(response.isValid());
        assertEquals("Minimum order value not met for this coupon", response.getMessage());
    }

    @Test
    void validateCoupon_NotFound() {
        CouponValidationRequest request = CouponValidationRequest.builder()
                .code("NONE")
                .orderValue(new BigDecimal("100.00"))
                .build();

        when(couponRepository.findByCodeIgnoreCase("NONE")).thenReturn(Optional.empty());

        assertThrows(CouponNotFoundException.class, () -> couponService.validateCoupon(request));
    }
}
