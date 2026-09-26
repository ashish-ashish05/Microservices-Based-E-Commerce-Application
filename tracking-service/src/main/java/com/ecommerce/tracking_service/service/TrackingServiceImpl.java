package com.ecommerce.tracking_service.service;

import com.ecommerce.tracking_service.dto.*;
import com.ecommerce.tracking_service.entity.*;
import com.ecommerce.tracking_service.exception.*;
import com.ecommerce.tracking_service.model.TrackingStatus;
import com.ecommerce.tracking_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {
    private final TrackingRepository trackingRepository;
    private final TrackingEventRepository eventRepository;

    @Override
    @Transactional
    public TrackingResponse initializeTracking(TrackingRequest request) {
        Tracking tracking = Tracking.builder()
                .shipmentId(request.getShipmentId())
                .trackingNumber(request.getTrackingNumber())
                .currentStatus(request.getInitialStatus())
                .updatedAt(LocalDateTime.now())
                .build();

        Tracking savedTracking = trackingRepository.save(tracking);

        TrackingEvent initialEvent = TrackingEvent.builder()
                .tracking(savedTracking)
                .status(request.getInitialStatus())
                .timestamp(LocalDateTime.now())
                .description("Shipment initialized")
                .build();
        eventRepository.save(initialEvent);

        return mapToTrackingResponse(savedTracking);
    }

    @Override
    @Transactional
    public TrackingResponse addTrackingEvent(String trackingNumber, TrackingEventRequest eventRequest) {
        Tracking tracking = trackingRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new TrackingNotFoundException(trackingNumber));

        TrackingStatus nextStatus = eventRequest.getStatus();
        if (!tracking.getCurrentStatus().canTransitionTo(nextStatus)) {
            throw new InvalidStatusTransitionException(
                    tracking.getCurrentStatus().name(),
                    nextStatus.name()
            );
        }

        TrackingEvent event = TrackingEvent.builder()
                .tracking(tracking)
                .status(nextStatus)
                .location(eventRequest.getLocation())
                .description(eventRequest.getDescription())
                .timestamp(eventRequest.getTimestamp() != null ? eventRequest.getTimestamp() : LocalDateTime.now())
                .build();
        eventRepository.save(event);

        tracking.setCurrentStatus(nextStatus);
        tracking.setUpdatedAt(LocalDateTime.now());
        trackingRepository.save(tracking);

        return mapToTrackingResponse(tracking);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingHistoryResponse getTrackingHistory(String trackingNumber) {
        Tracking tracking = trackingRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new TrackingNotFoundException(trackingNumber));

        List<TrackingEvent> events = eventRepository.findByTrackingIdOrderByTimestampAsc(tracking.getId());

        List<TrackingHistoryResponse.EventDTO> eventDtos = events.stream()
                .map(e -> TrackingHistoryResponse.EventDTO.builder()
                        .status(e.getStatus())
                        .location(e.getLocation())
                        .description(e.getDescription())
                        .timestamp(e.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return TrackingHistoryResponse.builder()
                .trackingNumber(tracking.getTrackingNumber())
                .currentStatus(tracking.getCurrentStatus())
                .events(eventDtos)
                .build();
    }

    private TrackingResponse mapToTrackingResponse(Tracking tracking) {
        return TrackingResponse.builder()
                .trackingNumber(tracking.getTrackingNumber())
                .currentStatus(tracking.getCurrentStatus())
                .updatedAt(tracking.getUpdatedAt())
                .build();
    }
}
