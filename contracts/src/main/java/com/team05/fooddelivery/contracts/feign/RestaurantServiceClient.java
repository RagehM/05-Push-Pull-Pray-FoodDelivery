package com.team05.fooddelivery.contracts.feign;

import com.team05.fooddelivery.contracts.dto.AvgPriceDTO;
import com.team05.fooddelivery.contracts.dto.RestaurantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service", url = "${feign.restaurant-service.url}")
public interface RestaurantServiceClient {

    @GetMapping("/api/restaurants/{id}")
    RestaurantDTO getRestaurant(@PathVariable Long id);

    @GetMapping("/api/restaurants/{id}/menu-items/avg-price")
    AvgPriceDTO getMenuItemAvgPrice(@PathVariable Long id);
}