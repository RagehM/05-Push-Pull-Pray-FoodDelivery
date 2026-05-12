package com.team05.fooddelivery.contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        Long restaurantId,
        String reason
) {}
