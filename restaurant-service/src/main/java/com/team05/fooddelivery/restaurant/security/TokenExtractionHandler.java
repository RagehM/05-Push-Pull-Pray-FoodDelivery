package com.team05.fooddelivery.restaurant.security;

import com.team05.shared.dto.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    public AuthContext handle(AuthContext ctx) {
        String header = ctx.request().getHeader("Authorization");
        System.out.println("HEADER : " + header);
        if (header == null || !header.startsWith("Bearer "))
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (TokenExtractor)");
        }
        String token = header.substring(7);
        return nextHandler.handle(new AuthContext(ctx.request(), token,null,ctx.requiredRole()));

    }
}
