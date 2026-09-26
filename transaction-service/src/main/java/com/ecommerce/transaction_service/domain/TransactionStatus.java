package com.ecommerce.transaction_service.domain;

import lombok.Getter;
import java.util.Set;

@Getter
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public boolean canTransitionTo(TransactionStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == SUCCESS || nextStatus == FAILED;
            case SUCCESS -> false; // Terminal state for this simplified model
            case FAILED -> false;  // Terminal state for this simplified model
        };
    }
}
