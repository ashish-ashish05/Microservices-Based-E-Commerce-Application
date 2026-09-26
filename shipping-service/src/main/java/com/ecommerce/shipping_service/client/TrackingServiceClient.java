package com.ecommerce.shipping_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "tracking-service", url = "${tracking.service.url:http://localhost:8096}")
public interface TrackingServiceClient {

    @PostMapping("/api/tracking")
    void createTrackingRecord(@RequestBody Map<String, Object> trackingData);
}
