package com.ecommerce.tracking_service.repository;

import com.ecommerce.tracking_service.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {
    List<TrackingEvent> findByTrackingIdOrderByTimestampAsc(UUID trackingId);
}
