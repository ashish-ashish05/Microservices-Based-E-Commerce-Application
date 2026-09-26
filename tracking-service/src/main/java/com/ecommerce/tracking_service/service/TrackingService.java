package com.ecommerce.tracking_service.service;

import com.ecommerce.tracking_service.dto.*;
import java.util.UUID;

public interface TrackingService {
    TrackingResponse initializeTracking(TrackingRequest request);
    TrackingResponse addTrackingEvent(String trackingNumber, TrackingEventRequest eventRequest);
    TrackingHistoryResponse getTrackingHistory(String trackingNumber);
}
