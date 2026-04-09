package com.team05.fooddelivery.order.dto;

public record OrderAnalyticsDTO(
    Long totalOrders,
    Long deliveredOrders,
    Long cancelledOrders,
    Double totalRevenue,
    Double averageOrderAmount,
    Double deliveryRate
) {}
