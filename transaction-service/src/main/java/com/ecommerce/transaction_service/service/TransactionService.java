package com.ecommerce.transaction_service.service;

import com.ecommerce.transaction_service.domain.Transaction;
import com.ecommerce.transaction_service.domain.TransactionStatus;
import com.ecommerce.transaction_service.dto.TransactionRequest;
import com.ecommerce.transaction_service.dto.TransactionResponse;
import com.ecommerce.transaction_service.exception.InvalidStateTransitionException;
import com.ecommerce.transaction_service.exception.TransactionNotFoundException;
import com.ecommerce.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse createOrUpdateTransaction(TransactionRequest request) {
        log.info("Creating or updating transaction for order: {}", request.getOrderId());

        // Check if a transaction already exists for this paymentId
        // In a real system, we might search by paymentId or providerTransactionId
        // For this simple impl, we'll allow creating multiple records but enforce state transitions if updating

        Transaction transaction = Transaction.builder()
                .orderId(request.getOrderId())
                .paymentId(request.getPaymentId())
                .providerTransactionId(request.getProviderTransactionId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(request.getStatus())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse updateStatus(UUID id, TransactionStatus nextStatus, String remarks) {
        log.info("Updating transaction {} status to {}", id, nextStatus);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getStatus().canTransitionTo(nextStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition transaction from %s to %s", transaction.getStatus(), nextStatus)
            );
        }

        transaction.setStatus(nextStatus);
        transaction.setRemarks(remarks);

        return mapToResponse(transactionRepository.save(transaction));
    }

    public List<TransactionResponse> getTransactionsByOrder(UUID orderId) {
        log.info("Fetching transactions for order: {}", orderId);
        return transactionRepository.findByOrderId(orderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .orderId(t.getOrderId())
                .paymentId(t.getPaymentId())
                .providerTransactionId(t.getProviderTransactionId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
