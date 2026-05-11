package com.team05.fooddelivery.contracts.dto;

import java.math.BigDecimal;

public record OrderDTO(
        Long id,
        Long userId,
        Long restaurantId,
        String status,
        String orderDate,
        String deliveredAt,
        BigDecimal totalAmount
) {}
