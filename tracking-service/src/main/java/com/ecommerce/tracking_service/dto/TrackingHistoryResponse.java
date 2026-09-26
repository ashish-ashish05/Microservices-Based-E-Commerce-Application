package com.ecommerce.tracking_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.ecommerce.tracking_service.model.TrackingStatus;
import java.util.List;

@Getter
@Setter
@Builder
public class TrackingHistoryResponse {
    private String trackingNumber;
    private TrackingStatus currentStatus;
    private List<EventDTO> events;

    @Getter
    @Setter
    @Builder
    public static class EventDTO {
        private TrackingStatus status;
        private String location;
        private String description;
        private java.time.LocalDateTime timestamp;
    }
}
