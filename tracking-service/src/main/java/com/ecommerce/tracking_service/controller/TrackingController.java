package com.ecommerce.tracking_service.controller;

import com.ecommerce.tracking_service.dto.*;
import com.ecommerce.tracking_service.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {
    private final TrackingService trackingService;

    @PostMapping
    public ResponseEntity<TrackingResponse> initializeTracking(@Valid @RequestBody TrackingRequest request) {
        return new ResponseEntity<>(trackingService.initializeTracking(request), HttpStatus.CREATED);
    }

    @PostMapping("/{trackingNumber}/events")
    public ResponseEntity<TrackingResponse> addTrackingEvent(
            @PathVariable String trackingNumber,
            @Valid @RequestBody TrackingEventRequest eventRequest) {
        return new ResponseEntity<>(trackingService.addTrackingEvent(trackingNumber, eventRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackingHistoryResponse> getTrackingHistory(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(trackingService.getTrackingHistory(trackingNumber));
    }
}
