package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public AuthContext handle(AuthContext ctx) {
        if (ctx.requiredRole() == null) {
            return ctx;
        }
        String requiredAuthority = "ROLE_" + ctx.requiredRole().name();
        boolean hasRole = ctx.user().getAuthorities().stream()
                .anyMatch(a -> requiredAuthority.equals(a.getAuthority()));
        if (!hasRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden (RoleAuthorization)");
        }
        return ctx;
    }

}
