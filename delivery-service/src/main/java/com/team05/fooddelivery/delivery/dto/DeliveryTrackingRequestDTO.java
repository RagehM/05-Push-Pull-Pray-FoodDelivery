package com.team05.fooddelivery.delivery.dto;

public record DeliveryTrackingRequestDTO(
        String status,
        Double latitude,
        Double longitude,
        String notes
) {}

