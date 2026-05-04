package com.team05.fooddelivery.restaurant.security;

import com.team05.shared.dto.AuthContext;
import com.team05.fooddelivery.restaurant.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

public class UserLoaderHandler extends AuthHandler {
    private final JwtService jwtService;

    public UserLoaderHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public AuthContext handle(AuthContext ctx) {
        String username = jwtService.extractUsername(ctx.token());
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (UserLoader): missing subject claim");
        }
        try {
            return nextHandler.handle(new AuthContext(ctx.request(), ctx.token(), null, ctx.requiredRole()));
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (UserLoader): user not found");
        }
    }

}
