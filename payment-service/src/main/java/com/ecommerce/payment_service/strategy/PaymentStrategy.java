package com.ecommerce.payment_service.strategy;

import com.ecommerce.payment_service.domain.Payment;
import com.ecommerce.payment_service.provider.PaymentProviderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

public interface PaymentStrategy {
    String getPaymentMethod();
    Optional<String> execute(Payment payment, PaymentProviderAdapter provider);
}
