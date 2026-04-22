package com.team05.fooddelivery.user.model;

public record LoginRequest(
        String email,
        String password
) {
}
