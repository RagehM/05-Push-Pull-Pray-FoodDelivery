package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import com.team05.fooddelivery.user.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

public class UserLoaderHandler extends AuthHandler {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public UserLoaderHandler(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public AuthContext handle(AuthContext ctx) {
        String username = jwtService.extractUsername(ctx.token());
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (UserLoader): missing subject claim");
        }
        try {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return nextHandler.handle(new AuthContext(ctx.request(), ctx.token(), userDetails, ctx.requiredRole()));
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (UserLoader): user not found");
        }
    }

}
