package com.ecommerce.payment_service.provider;

import com.ecommerce.payment_service.domain.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class MockPaymentProviderAdapter implements PaymentProviderAdapter {

    @Override
    public Optional<String> processPayment(Payment payment) {
        log.info("Mocking payment request for order: {}, amount: {} {}",
                 payment.getOrderId(), payment.getAmount(), payment.getCurrency());

        // Simulate some business logic for failure/success
        // For example, if amount is exactly 999.99, simulate a failure
        if (payment.getAmount().doubleValue() == 999.99) {
            log.warn("Simulating payment failure for order: {}", payment.getOrderId());
            return Optional.empty();
        }

        String mockTxnId = "MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Payment processed successfully. Provider Txn ID: {}", mockTxnId);
        return Optional.of(mockTxnId);
    }
}
