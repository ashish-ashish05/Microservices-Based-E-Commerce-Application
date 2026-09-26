package com.ecommerce.shipping_service.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(UUID id) {
        super("Shipment not found with id: " + id);
    }
}
