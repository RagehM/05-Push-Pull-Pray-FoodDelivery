package com.team05.fooddelivery.delivery.dto;

import java.time.LocalDateTime;

public record DelayedDeliveryDTO(
        Long deliveryId,
        String driverName,
        Long orderId,
        Double latitude,
        Double longitude,
        Double estimatedArrival,
        LocalDateTime updatedAt
) {}