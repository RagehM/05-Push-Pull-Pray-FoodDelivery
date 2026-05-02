package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

public class RoleAuthorizationHandler extends AuthHandler {


    AuthContext handle(AuthContext ctx)
    {
        System.out.println("ROLE AUTHORIZATION : "+ctx.requiredRole());
        SimpleGrantedAuthority requiredRole = new SimpleGrantedAuthority("ROLE_ADMIN");
        if(ctx.requiredRole()!= null && !ctx.user().getAuthorities().contains(requiredRole))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden (RoleAuthorization)");
        }
        return ctx;
    }

}
