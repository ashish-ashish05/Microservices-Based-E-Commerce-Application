package com.ecommerce.tracking_service.entity;

import com.ecommerce.tracking_service.model.TrackingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracking", indexes = {
    @Index(name = "idx_shipment_id", columnList = "shipmentId"),
    @Index(name = "idx_tracking_number", columnList = "trackingNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tracking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID shipmentId;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingStatus currentStatus;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
