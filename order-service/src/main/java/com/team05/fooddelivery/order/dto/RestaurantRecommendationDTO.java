package com.team05.fooddelivery.order.dto;

public record RestaurantRecommendationDTO(
        Long restaurantId,
        String name,
        String cuisineType,
        Long score
) {}
