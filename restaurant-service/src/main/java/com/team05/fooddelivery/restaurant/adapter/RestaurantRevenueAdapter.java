package com.team05.fooddelivery.restaurant.adapter;

import com.team05.fooddelivery.restaurant.dto.RestaurantRevenueDTO;
import com.team05.fooddelivery.restaurant.model.Restaurant;

// Section 3.8 — Adapter Pattern
// Converts Object[] raw SQL projection result into RestaurantRevenueDTO
// Used by S2-F3 (Get Restaurant Order Revenue Summary)
public class RestaurantRevenueAdapter {

    private final Object[] row;
    private final Restaurant restaurant;

    public RestaurantRevenueAdapter(Object[] row, Restaurant restaurant) {
        this.row = row;
        this.restaurant = restaurant;
    }

    public RestaurantRevenueDTO toDTO() {
        Long totalOrders = ((Number) row[0]).longValue();
        Double totalRevenue = ((Number) row[1]).doubleValue();
        Double averageOrderAmount = ((Number) row[2]).doubleValue();

        return RestaurantRevenueDTO.builder()
                .restaurantId(restaurant.getId())
                .name(restaurant.getName())
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderAmount(averageOrderAmount)
                .build();
    }
}