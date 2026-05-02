package com.team05.fooddelivery.order.security;

public record JwtUserPrincipal(
        Long uid,
        String username,
        String role
) {
}

