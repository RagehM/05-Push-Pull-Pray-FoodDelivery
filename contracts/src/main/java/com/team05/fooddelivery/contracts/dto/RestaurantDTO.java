package com.team05.fooddelivery.contracts.dto;

public record RestaurantDTO(
        Long id,
        String name,
        String cuisineType,
        Double rating,
        String status
) {}
