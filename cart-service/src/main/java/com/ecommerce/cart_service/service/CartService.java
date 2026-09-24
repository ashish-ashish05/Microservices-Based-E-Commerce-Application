package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.dto.CartResponse;
import com.ecommerce.cart_service.dto.CartRequest;

public interface CartService {
    CartResponse getCart(String userId);
    CartResponse addItem(String userId, CartRequest request);
    CartResponse updateQuantity(String userId, java.util.UUID productId, Integer quantity);
    void removeItem(String userId, java.util.UUID productId);
    void clearCart(String userId);
}
