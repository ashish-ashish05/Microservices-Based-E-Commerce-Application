package com.ecommerce.tracking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.ecommerce.tracking_service.model.TrackingStatus;
import java.util.UUID;

@Getter
@Setter
public class TrackingRequest {
    @NotNull
    private UUID shipmentId;
    @NotBlank
    private String trackingNumber;
    @NotNull
    private TrackingStatus initialStatus;
}
