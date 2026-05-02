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


    AuthContext handle(AuthContext ctx)
    {
        String username = jwtService.extractUsername(ctx.token());
        try
        {
            if(username != null)
            {
                System.out.println("Username EXTRACTED: " + username);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println("USER RETRIEVED : " + userDetails.getUsername());
                return nextHandler.handle(new AuthContext(ctx.request(), ctx.token(), userDetails,ctx.requiredRole()));
            }
        }
        catch (UsernameNotFoundException e)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (UserLoader)");
        }
        catch(Exception e)
        {
            throw e;
        }

        return ctx;
    }

}
