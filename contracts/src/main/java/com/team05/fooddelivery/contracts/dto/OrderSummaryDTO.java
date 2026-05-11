package com.team05.fooddelivery.contracts.dto;

import java.math.BigDecimal;

public record OrderSummaryDTO(
        Long totalOrders,
        Long deliveredOrders,
        Long cancelledOrders,
        BigDecimal totalSpent,
        BigDecimal avgOrderAmount
) {
    public static OrderSummaryDTO empty() {
        return new OrderSummaryDTO(0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
