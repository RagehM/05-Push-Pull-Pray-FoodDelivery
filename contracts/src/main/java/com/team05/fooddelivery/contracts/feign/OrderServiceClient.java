package com.team05.fooddelivery.contracts.feign;

import com.team05.fooddelivery.contracts.dto.OrderDTO;
import com.team05.fooddelivery.contracts.dto.OrderSummaryDTO;
import com.team05.fooddelivery.contracts.dto.RestaurantOrderSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {

    @GetMapping("/api/orders/user/{userId}/summary")
    OrderSummaryDTO getUserOrderSummary(@PathVariable Long userId);

    @GetMapping("/api/orders/user/{userId}/active-count")
    int getActiveOrderCount(@PathVariable Long userId);

    @GetMapping("/api/orders/user/{userId}/count")
    long getTotalOrderCount(@PathVariable Long userId);

    @GetMapping("/api/orders/restaurant/{restaurantId}/summary")
    RestaurantOrderSummaryDTO getRestaurantOrderSummary(@PathVariable Long restaurantId);

    @GetMapping("/api/orders/{orderId}")
    OrderDTO getOrder(@PathVariable Long orderId);

    @GetMapping("/api/orders/restaurant/{restaurantId}/active-count")
    int getActiveOrderCountByRestaurant(@PathVariable Long restaurantId);
}