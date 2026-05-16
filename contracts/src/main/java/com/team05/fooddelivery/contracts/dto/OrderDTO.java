package com.team05.fooddelivery.contracts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDTO(
        Long id,
        Long userId,
        Long restaurantId,
        String status,
        LocalDateTime orderDate,
        LocalDateTime deliveredAt,
        BigDecimal totalAmount
) { }
