package com.team05.fooddelivery.user.dto;

import java.util.List;
import java.util.Map;

public record UserProfileDTO(
        Long userId,
        String name,
        String email,
        String phone,
        Map<String, Object> preferences,
        List<DeliveryAddressDTO> deliveryAddresses,
        Integer totalAddresses
) {
}
