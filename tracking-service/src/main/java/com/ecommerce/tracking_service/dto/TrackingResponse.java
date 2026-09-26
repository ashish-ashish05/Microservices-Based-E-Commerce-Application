package com.ecommerce.tracking_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.ecommerce.tracking_service.model.TrackingStatus;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TrackingResponse {
    private String trackingNumber;
    private TrackingStatus currentStatus;
    private LocalDateTime updatedAt;
}
