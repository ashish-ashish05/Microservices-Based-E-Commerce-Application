package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.client.PricingClient;
import com.ecommerce.cart_service.client.ProductClient;
import com.ecommerce.cart_service.dto.*;
import com.ecommerce.cart_service.entity.Cart;
import com.ecommerce.cart_service.entity.CartItem;
import com.ecommerce.cart_service.exception.*;
import com.ecommerce.cart_service.repository.CartItemRepository;
import com.ecommerce.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final PricingClient pricingClient;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        // Prepare request for Pricing Service
        List<CartPricingRequest.ItemRequest> pricingItems = items.stream()
                .map(item -> CartPricingRequest.ItemRequest.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        CartPricingRequest pricingRequest = CartPricingRequest.builder()
                .items(pricingItems)
                .build();

        CartPricingResponse pricingResponse = pricingClient.calculateCart(pricingRequest);

        // Map local items and pricing breakdown to response
        List<CartItemResponse> itemResponses = items.stream().map(item -> {
            CartPricingResponse.PricingBreakdown breakdown = pricingResponse.getBreakdown().stream()
                    .filter(b -> b.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .orElse(CartPricingResponse.PricingBreakdown.builder()
                            .itemTotal(BigDecimal.ZERO)
                            .discount(BigDecimal.ZERO)
                            .build());

            // Fetch product metadata (name)
            ProductResponse product = productClient.getProduct(item.getProductId());

            return CartItemResponse.builder()
                    .productId(item.getProductId())
                    .productName(product != null ? product.getName() : "Unknown Product")
                    .quantity(item.getQuantity())
                    .unitPrice(breakdown.getItemTotal().divide(BigDecimal.valueOf(item.getQuantity()), 2, java.math.RoundingMode.HALF_UP))
                    .subTotal(breakdown.getItemTotal().subtract(breakdown.getDiscount()))
                    .build();
        }).collect(Collectors.toList());

        return CartResponse.builder()
                .userId(userId)
                .items(itemResponses)
                .totalPrice(pricingResponse.getFinalTotal())
                .totalItems(items.size())
                .currency(pricingResponse.getCurrency())
                .build();
    }

    @Override
    @Transactional
    public CartResponse addItem(String userId, CartRequest request) {
        // Validate product existence
        try {
            productClient.getProduct(request.getProductId());
        } catch (Exception e) {
            throw new ProductNotFoundException("Product not found: " + request.getProductId());
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(String userId, java.util.UUID productId, Integer quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be at least 1");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Override
    @Transactional
    public void removeItem(String userId, java.util.UUID productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cartItemRepository.deleteAll(items);
    }
}
