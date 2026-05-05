package com.team05.fooddelivery.restaurant.adapter;

import com.team05.fooddelivery.restaurant.dto.TopRestaurantDTO;

// Section 3.8 — Adapter Pattern
// Converts Object[] raw SQL projection result into TopRestaurantDTO
// Used by S2-F6 (Top Rated Restaurants Report)
public class TopRestaurantAdapter {

    private final Object[] row;

    public TopRestaurantAdapter(Object[] row) {
        this.row = row;
    }

    public TopRestaurantDTO adapt(Object[] row) {
        Long id = ((Number) this.row[0]).longValue();
        String name = (String) this.row[1];
        Double rating = ((Number) this.row[2]).doubleValue();
        Long totalOrders = ((Number) this.row[3]).longValue();

        return TopRestaurantDTO.builder()
                .restaurantId(id)
                .name(name)
                .rating(rating)
                .totalOrders(totalOrders)
                .build();
    }
}