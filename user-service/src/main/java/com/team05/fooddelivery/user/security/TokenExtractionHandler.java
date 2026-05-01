package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import com.team05.fooddelivery.user.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    AuthContext handle(AuthContext ctx) {
        String header = ctx.request().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (TokenExtractor)");
        }
        String token = header.substring(7);
        return nextHandler.handle(new AuthContext(ctx.request(), token,null,ctx.requiredRole()));

    }
}
