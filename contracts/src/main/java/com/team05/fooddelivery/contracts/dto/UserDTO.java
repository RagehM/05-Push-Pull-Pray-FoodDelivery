package com.team05.fooddelivery.contracts.dto;

import java.util.Map;

public record UserDTO(
        Long id,
        String email,
        String role,
        String status,
        Map<String, String> preferences
) {}
