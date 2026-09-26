package com.ecommerce.tracking_service.repository;

import com.ecommerce.tracking_service.entity.Tracking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TrackingRepository extends JpaRepository<Tracking, UUID> {
    Optional<Tracking> findByTrackingNumber(String trackingNumber);
    Optional<Tracking> findByShipmentId(UUID shipmentId);
}
