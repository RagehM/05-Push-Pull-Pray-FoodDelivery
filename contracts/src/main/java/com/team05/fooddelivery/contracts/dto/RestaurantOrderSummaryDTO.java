package com.team05.fooddelivery.contracts.dto;

import java.math.BigDecimal;

public record RestaurantOrderSummaryDTO(
        Long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal avgOrderValue
) {
    public static RestaurantOrderSummaryDTO empty() {
        return new RestaurantOrderSummaryDTO(0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
