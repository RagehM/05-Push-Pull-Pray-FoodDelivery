package com.team05.fooddelivery.user.dto;

import com.team05.fooddelivery.user.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthContext(HttpServletRequest request,
                          String token,
                          UserDetails user,
                          UserRole requiredRole) {
}
