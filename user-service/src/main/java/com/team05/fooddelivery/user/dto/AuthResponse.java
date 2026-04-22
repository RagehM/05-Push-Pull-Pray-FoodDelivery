package com.team05.fooddelivery.user.dto;

public record AuthResponse(
        String token,
        long expiresIn
) {
}
