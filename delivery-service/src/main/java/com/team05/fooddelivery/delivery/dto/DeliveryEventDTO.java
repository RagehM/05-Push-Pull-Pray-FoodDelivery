package com.team05.fooddelivery.delivery.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DeliveryEventDTO(
        String id,
        Long deliveryId,
        String action,
        LocalDateTime timestamp,
        Map<String, Object> details
) {}
