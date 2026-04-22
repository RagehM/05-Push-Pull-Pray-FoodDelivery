package com.team05.fooddelivery.user.dto;

public record LoginRequest(
        String email,
        String password
) {
}
