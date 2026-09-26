package com.ecommerce.transaction_service.controller;

import com.ecommerce.transaction_service.domain.TransactionStatus;
import com.ecommerce.transaction_service.dto.TransactionRequest;
import com.ecommerce.transaction_service.dto.TransactionResponse;
import com.ecommerce.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return new ResponseEntity<>(transactionService.createOrUpdateTransaction(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TransactionResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam TransactionStatus status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(transactionService.updateStatus(id, status, remarks));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(transactionService.getTransactionsByOrder(orderId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
