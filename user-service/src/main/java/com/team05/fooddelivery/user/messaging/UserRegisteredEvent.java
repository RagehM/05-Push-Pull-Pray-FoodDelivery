package com.team05.fooddelivery.user.messaging;


import com.team05.fooddelivery.user.enums.UserRole;

public record UserRegisteredEvent(Long userId,
                                  String email,
                                  UserRole role) {
}
