package com.team05.fooddelivery.restaurant.dto;

// [S2-F6] Response DTO for the top-rated restaurants report endpoint.
// Contains the restaurant's ID, name, rating, and total order count.
public record TopRestaurantDTO

(Long restaurantId,
        String name,
        Double rating,
        Long totalOrders) {

}