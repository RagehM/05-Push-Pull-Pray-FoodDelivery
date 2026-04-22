package com.team05.fooddelivery.user.model;

public record AuthResponse(
        String token,
        long expiresIn
) {
}
