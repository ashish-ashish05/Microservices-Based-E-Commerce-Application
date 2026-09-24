package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ShippingRequest;
import com.ecommerce.order_service.dto.ShippingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "shipping-service", url = "${services.shipping.url}")
public interface ShippingClient {
    @PostMapping("/shipping/create")
    ShippingResponse createShipment(@RequestBody ShippingRequest request);
}
