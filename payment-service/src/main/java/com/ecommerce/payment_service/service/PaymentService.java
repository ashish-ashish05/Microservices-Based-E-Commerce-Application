package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.domain.Payment;
import com.ecommerce.payment_service.domain.PaymentStatus;
import com.ecommerce.payment_service.dto.PaymentRequest;
import com.ecommerce.payment_service.dto.PaymentResponse;
import com.ecommerce.payment_service.dto.PaymentStatusResponse;
import com.ecommerce.payment_service.exception.*;
import com.ecommerce.payment_service.provider.PaymentProviderAdapter;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderAdapter providerAdapter;
    private final List<PaymentStrategy> paymentStrategies;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment request for order: {}, idempotencyKey: {}",
                 request.getOrderId(), request.getIdempotencyKey());

        // 1. Idempotency Check
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            log.warn("Duplicate payment request detected for idempotencyKey: {}. Returning existing payment {}.",
                      request.getIdempotencyKey(), payment.getId());
            return mapToPaymentResponse(payment);
        }

        // 2. Create Initial Pending Payment
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Created pending payment: {} for order: {}", payment.getId(), payment.getOrderId());

        // 3. Execute Payment via Strategy
        PaymentStrategy strategy = paymentStrategies.stream()
                .filter(s -> s.getPaymentMethod().equalsIgnoreCase(request.getPaymentMethod()))
                .findFirst()
                .orElseThrow(() -> new InvalidPaymentRequestException("Unsupported payment method: " + request.getPaymentMethod()));

        try {
            Optional<String> transactionId = strategy.execute(payment, providerAdapter);

            if (transactionId.isPresent()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderTxnId(transactionId.get());
                log.info("Payment successful for payment: {}. Provider Txn ID: {}", payment.getId(), transactionId.get());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                log.warn("Payment failed for payment: {}. Provider returned no transaction ID.", payment.getId());
            }
        } catch (Exception e) {
            log.error("Provider error during payment processing for payment: {}", payment.getId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            // We wrap provider exceptions into our domain exception
            throw new ProviderUnavailableException("Payment provider error: " + e.getMessage());
        }

        payment = paymentRepository.save(payment);
        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID paymentId) {
        log.info("Retrieving status for payment: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        return mapToPaymentStatusResponse(payment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .transactionId(payment.getProviderTxnId())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PaymentStatusResponse mapToPaymentStatusResponse(Payment payment) {
        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
