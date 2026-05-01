package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import com.team05.fooddelivery.user.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    AuthContext handle(AuthContext ctx)
    {
        if(!jwtService.isTokenValid(ctx.token()))
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized (SignatureValidation)");
        }
        else
        {
            return nextHandler.handle(ctx);
        }
    }

}
