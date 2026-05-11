package com.team05.fooddelivery.contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeliveryCreatedEvent(
        Long deliveryId,
        Long orderId,
        Long restaurantId,
        String driverName
) {}
