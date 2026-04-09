package com.team05.fooddelivery.delivery.dto;

import java.util.List;

public record BatchDeliveryRequestDTO(
        Long orderId,
        List<DeliveryItemDTO> deliveries
) {}
