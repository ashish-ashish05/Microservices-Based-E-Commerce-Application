package com.ecommerce.payment_service.strategy;

import com.ecommerce.payment_service.domain.Payment;
import com.ecommerce.payment_service.provider.PaymentProviderAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Slf4j
@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public String getPaymentMethod() {
        return "CREDIT_CARD";
    }

    @Override
    public Optional<String> execute(Payment payment, PaymentProviderAdapter provider) {
        log.info("Executing Credit Card payment strategy for order: {}", payment.getOrderId());
        return provider.processPayment(payment);
    }
}
