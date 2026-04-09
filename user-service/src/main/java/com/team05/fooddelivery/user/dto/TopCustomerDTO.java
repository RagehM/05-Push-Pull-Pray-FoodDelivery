package com.team05.fooddelivery.user.dto;

public record TopCustomerDTO(Long userId,
                             String name,
                             Double totalSpent,
                             Integer orderCount) {
}
