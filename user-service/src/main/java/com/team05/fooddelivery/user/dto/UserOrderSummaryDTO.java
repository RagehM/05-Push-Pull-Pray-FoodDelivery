package com.team05.fooddelivery.user.dto;

import java.util.List;

public record UserOrderSummaryDTO(
        Long userId,
        String name,
        Integer totalOrders,
        Integer deliveredOrders,
        Integer cancelledOrders,
        Double totalSpent,
        Double averageOrderAmount
) {
}
