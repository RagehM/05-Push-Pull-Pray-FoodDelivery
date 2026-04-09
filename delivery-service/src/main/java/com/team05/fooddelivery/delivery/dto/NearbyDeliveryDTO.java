package com.team05.fooddelivery.delivery.dto;

public record NearbyDeliveryDTO(Long deliveryId, String driverName, Long orderId, Double latitude, Double longitude,
                                Double distanceKm) {
}