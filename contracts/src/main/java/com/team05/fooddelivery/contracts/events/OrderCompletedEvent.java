package com.team05.fooddelivery.contracts.events;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCompletedEvent(
        Long orderId,
        Long userId,
        Long restaurantId,
        BigDecimal totalAmount
) {}
