package com.ecommerce.tracking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.ecommerce.tracking_service.model.TrackingStatus;
import java.time.LocalDateTime;

@Getter
@Setter
public class TrackingEventRequest {
    @NotNull
    private TrackingStatus status;
    private String location;
    private String description;
    private LocalDateTime timestamp;
}
