package com.team05.fooddelivery.contracts.feign;

import com.team05.fooddelivery.contracts.dto.DeliveryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "delivery-service", url = "${feign.delivery-service.url}")
public interface DeliveryServiceClient {

    @GetMapping("/api/deliveries/order/{orderId}/active")
    DeliveryDTO getActiveDeliveryForOrder(@PathVariable Long orderId);
}
