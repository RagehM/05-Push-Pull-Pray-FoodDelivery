package com.team05.fooddelivery.contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestaurantStatusChangedEvent(
        Long restaurantId,
        String oldStatus,
        String newStatus
) {}
