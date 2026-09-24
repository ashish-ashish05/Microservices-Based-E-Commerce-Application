package com.ecommerce.pricing_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "coupon-service", url = "${coupon.service.url:http://localhost:8086}")
public interface CouponServiceClient {

    @GetMapping("/coupons/{code}/discount")
    Double getDiscountValue(@PathVariable("code") String code);
}
