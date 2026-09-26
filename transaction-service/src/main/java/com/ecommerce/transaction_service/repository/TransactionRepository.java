package com.ecommerce.transaction_service.repository;

import com.ecommerce.transaction_service.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByOrderId(UUID orderId);
}
