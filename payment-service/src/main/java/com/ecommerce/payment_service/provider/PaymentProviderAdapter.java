package com.ecommerce.payment_service.provider;

import com.ecommerce.payment_service.domain.Payment;
import java.util.Optional;

public interface PaymentProviderAdapter {
    Optional<String> processPayment(Payment payment);
}
