package com.team05.shared.dto;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthContext(HttpServletRequest request,
                          String token,
                          UserDetails user,
                          String requiredRole) {
}
