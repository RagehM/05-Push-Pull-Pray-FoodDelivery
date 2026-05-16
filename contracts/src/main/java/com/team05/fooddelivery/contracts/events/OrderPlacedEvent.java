package com.team05.fooddelivery.contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

// Routing Key -> order.placed
// Listened to by: restaurant-service, delivery-service
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderPlacedEvent(
        Long orderId,
        Long userId,
        Long restaurantId,
        BigDecimal totalAmount
) {}
