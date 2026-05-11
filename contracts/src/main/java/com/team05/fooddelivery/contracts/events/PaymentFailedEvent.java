package com.team05.fooddelivery.contracts.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        String reason
) {}
