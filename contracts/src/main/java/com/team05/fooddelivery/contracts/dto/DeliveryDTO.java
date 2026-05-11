package com.team05.fooddelivery.contracts.dto;

public record DeliveryDTO(
        Long id,
        Long orderId,
        String status,
        String driverName,
        Long restaurantId
) {}
