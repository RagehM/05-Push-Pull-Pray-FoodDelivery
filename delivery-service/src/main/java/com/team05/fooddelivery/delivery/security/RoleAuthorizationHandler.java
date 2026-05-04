package com.team05.fooddelivery.delivery.security;

import com.team05.fooddelivery.delivery.config.JwtConfigurationManager;
import com.team05.fooddelivery.delivery.security.AuthHandler;
import com.team05.fooddelivery.delivery.service.JwtService;
import com.team05.shared.dto.AuthContext;
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
