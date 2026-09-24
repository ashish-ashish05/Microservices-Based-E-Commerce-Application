package com.ecommerce.coupon_service.controller;

import com.ecommerce.coupon_service.dto.request.CouponRequest;
import com.ecommerce.coupon_service.dto.request.CouponValidationRequest;
import com.ecommerce.coupon_service.dto.response.CouponResponse;
import com.ecommerce.coupon_service.dto.response.CouponValidationResponse;
import com.ecommerce.coupon_service.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        return new ResponseEntity<>(couponService.createCoupon(request), HttpStatus.CREATED);
    }

    @GetMapping("/{code}")
    public ResponseEntity<CouponResponse> getCouponDetails(@PathVariable String code) {
        return ResponseEntity.ok(couponService.getCouponDetails(code));
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(@Valid @RequestBody CouponValidationRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(request));
    }

    @PostMapping("/use")
    public ResponseEntity<Void> useCoupon(@RequestParam String code) {
        couponService.useCoupon(code);
        return ResponseEntity.ok().build();
    }
}
