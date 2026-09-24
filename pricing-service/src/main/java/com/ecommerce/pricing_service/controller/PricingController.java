package com.ecommerce.pricing_service.controller;

import com.ecommerce.pricing_service.dto.request.CartPricingRequest;
import com.ecommerce.pricing_service.dto.response.CartPricingResponse;
import com.ecommerce.pricing_service.dto.response.PriceResponse;
import com.ecommerce.pricing_service.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/calculate/{productId}")
    public ResponseEntity<PriceResponse> calculateProductPrice(@PathVariable UUID productId) {
        return ResponseEntity.ok(pricingService.calculateProductPrice(productId));
    }

    @PostMapping("/calculate-cart")
    public ResponseEntity<CartPricingResponse> calculateCartTotal(@Valid @RequestBody CartPricingRequest request) {
        return ResponseEntity.ok(pricingService.calculateCartTotal(request));
    }
}
