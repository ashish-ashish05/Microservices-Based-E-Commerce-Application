package com.ecommerce.tracking_service.model;

import lombok.Getter;
import java.util.Set;
import java.util.Collections;

@Getter
public enum TrackingStatus {
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION;

    public boolean canTransitionTo(TrackingStatus nextStatus) {
        if (this == nextStatus) return true;
        if (this == DELIVERED) return false; // Terminal state
        if (this == EXCEPTION) return true;    // Can recover from exception to any state

        return switch (this) {
            case PICKED_UP -> Set.of(IN_TRANSIT, EXCEPTION).contains(nextStatus);
            case IN_TRANSIT -> Set.of(OUT_FOR_DELIVERY, EXCEPTION).contains(nextStatus);
            case OUT_FOR_DELIVERY -> Set.of(DELIVERED, EXCEPTION).contains(nextStatus);
            default -> false;
        };
    }
}
