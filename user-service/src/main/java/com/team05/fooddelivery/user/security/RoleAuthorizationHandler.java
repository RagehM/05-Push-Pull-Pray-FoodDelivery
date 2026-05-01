package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RoleAuthorizationHandler extends AuthHandler {


    AuthContext handle(AuthContext ctx)
    {
        if(!ctx.user().getAuthorities().contains(ctx.requiredRole()))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden (RoleAuthorization)");
        }
        return ctx;
    }

}
