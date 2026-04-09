package com.team05.fooddelivery.delivery.dto;

import java.time.LocalDateTime;

public record DeliveryPerformanceSummaryDTO(
        String driverName,
        long totalDeliveries,
        double averageSpeed,
        double maxSpeed,
        LocalDateTime firstDelivery,
        LocalDateTime lastDelivery
) {}
