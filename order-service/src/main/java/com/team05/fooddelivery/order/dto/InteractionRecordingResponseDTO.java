package com.team05.fooddelivery.order.dto;

public record InteractionRecordingResponseDTO(
        Long orderId,
        Long userId,
        Long restaurantId,
        String message,
        boolean isIdempotent
) {}