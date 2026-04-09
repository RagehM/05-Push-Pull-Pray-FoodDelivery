package com.team05.fooddelivery.delivery.dto;

import java.util.Map;

import com.team05.fooddelivery.delivery.enums.DeliveryStatus;

public record DeliveryItemDTO(
        String driverName,
        Double latitude,
        Double longitude,
        DeliveryStatus status,
        Map<String, Object> metadata
) {}
