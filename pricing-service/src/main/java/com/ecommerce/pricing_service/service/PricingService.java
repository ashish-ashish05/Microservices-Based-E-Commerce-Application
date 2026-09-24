package com.ecommerce.pricing_service.service;

import com.ecommerce.pricing_service.client.CouponServiceClient;
import com.ecommerce.pricing_service.dto.request.CartPricingRequest;
import com.ecommerce.pricing_service.dto.response.CartPricingResponse;
import com.ecommerce.pricing_service.dto.response.PriceResponse;
import com.ecommerce.pricing_service.dto.response.CartPricingResponse.CartItemPricingBreakdown;
import com.ecommerce.pricing_service.exception.ProductNotFoundException;
import com.ecommerce.pricing_service.handler.*;
import com.ecommerce.pricing_service.strategy.PricingStrategy;
import com.ecommerce.pricing_service.strategy.PricingStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingStrategyFactory strategyFactory;
    private final GlobalDiscountHandler globalHandler;
    private final ProductDiscountHandler productHandler;
    private final CouponDiscountHandler couponHandler;

    // Mocked Product Service base price for now, as Product Service might not have the endpoint yet
    private BigDecimal getBasePrice(UUID productId) {
        if (productId == null) throw new ProductNotFoundException("Product ID cannot be null");
        return new BigDecimal("100.00"); // Default base price
    }

    public PriceResponse calculateProductPrice(UUID productId) {
        BigDecimal basePrice = getBasePrice(productId);

        // Initialize chain
        globalHandler.setNext(productHandler);
        productHandler.setNext(couponHandler);

        PricingContext context = new PricingContext(productId, null);
        BigDecimal finalPrice = globalHandler.handle(context, basePrice);

        return PriceResponse.builder()
                .productId(productId)
                .basePrice(basePrice)
                .finalPrice(finalPrice.setScale(2, RoundingMode.HALF_UP))
                .discountApplied(basePrice.subtract(finalPrice))
                .currency("USD")
                .build();
    }

    public CartPricingResponse calculateCartTotal(CartPricingRequest request) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<CartItemPricingBreakdown> breakdown = new ArrayList<>();

        for (CartPricingRequest.CartItemDTO item : request.getItems()) {
            BigDecimal basePrice = getBasePrice(item.getProductId());
            PricingStrategy strategy = strategyFactory.getStrategy(item.getQuantity());
            BigDecimal itemBaseTotal = strategy.calculate(basePrice, item.getQuantity());

            // Apply Global and Product discounts to the item
            globalHandler.setNext(productHandler);
            productHandler.setNext(null); // End chain for item level

            PricingContext itemContext = new PricingContext(item.getProductId(), null);
            BigDecimal itemFinalPrice = globalHandler.handle(itemContext, itemBaseTotal);

            BigDecimal itemDiscount = itemBaseTotal.subtract(itemFinalPrice);

            subTotal = subTotal.add(itemBaseTotal);
            totalDiscount = totalDiscount.add(itemDiscount);

            breakdown.add(CartItemPricingBreakdown.builder()
                    .productId(item.getProductId())
                    .itemTotal(itemBaseTotal.setScale(2, RoundingMode.HALF_UP))
                    .discount(itemDiscount.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        BigDecimal cartTotalAfterItemDiscounts = subTotal.subtract(totalDiscount);

        // Apply Coupon Discount to the whole cart
        couponHandler.setNext(null);
        PricingContext cartContext = new PricingContext(null, request.getCouponCode());
        BigDecimal finalCartTotal = couponHandler.handle(cartContext, cartTotalAfterItemDiscounts);

        BigDecimal couponDiscount = cartTotalAfterItemDiscounts.subtract(finalCartTotal);
        totalDiscount = totalDiscount.add(couponDiscount);

        return CartPricingResponse.builder()
                .subTotal(subTotal.setScale(2, RoundingMode.HALF_UP))
                .discountTotal(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .finalTotal(finalCartTotal.setScale(2, RoundingMode.HALF_UP))
                .currency("USD")
                .breakdown(breakdown)
                .build();
    }
}
