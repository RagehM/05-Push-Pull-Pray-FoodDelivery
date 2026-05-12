package com.team05.fooddelivery.contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestaurantRatedEvent(
        Long restaurantId,
        Long orderId,
        Double rating,
        Long userId
) {}
