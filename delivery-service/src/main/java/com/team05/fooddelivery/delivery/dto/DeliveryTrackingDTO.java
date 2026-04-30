package com.team05.fooddelivery.delivery.dto;

import java.time.Instant;

public record DeliveryTrackingDTO(
        Instant timestamp,
        String status,
        String driverName,
        Double latitude,
        Double longitude,
        String notes
) {}
