package com.team05.fooddelivery.order.dto;

public record OrderEstimateRequest(
        Long restaurantId,
        Integer itemCount,
        Double deliveryDistance
) {}
