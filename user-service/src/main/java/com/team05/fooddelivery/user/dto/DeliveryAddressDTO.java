package com.team05.fooddelivery.user.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DeliveryAddressDTO(
        Long id,
        String label,
        String streetAddress,
        String city,
        Double latitude,
        Double longitude,
        Boolean isDefault,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
}