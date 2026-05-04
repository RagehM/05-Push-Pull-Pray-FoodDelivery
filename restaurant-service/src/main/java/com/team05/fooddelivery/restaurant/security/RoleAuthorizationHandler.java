package com.team05.fooddelivery.restaurant.security;

import com.team05.fooddelivery.restaurant.config.JwtConfigurationManager;
import com.team05.fooddelivery.restaurant.service.JwtService;
import com.team05.shared.dto.AuthContext;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RoleAuthorizationHandler extends AuthHandler {
    private final JwtService jwtService;
    private final JwtConfigurationManager jwtConfigurationManager;

    public RoleAuthorizationHandler(JwtService jwtService, JwtConfigurationManager jwtConfigurationManager) {
        this.jwtService = jwtService;
        this.jwtConfigurationManager = jwtConfigurationManager;
    }


    @Override
    public AuthContext handle(AuthContext ctx) {
        if (ctx.requiredRole() == null) {
            return ctx;
        }
        String role = jwtService
                .getAllClaims(ctx.token(), jwtConfigurationManager.getInstance().getSecret()).get("role", String.class);
        boolean hasRole = role == ctx.requiredRole();
        if (!hasRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden (RoleAuthorization)");
        }
        return ctx;
    }

}
