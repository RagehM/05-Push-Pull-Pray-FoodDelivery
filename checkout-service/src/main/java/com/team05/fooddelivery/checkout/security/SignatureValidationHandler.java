package com.team05.fooddelivery.checkout.security;

import com.team05.fooddelivery.checkout.security.AuthHandler;
import com.team05.fooddelivery.checkout.service.JwtService;
import com.team05.shared.dto.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    @Override
    public AuthContext handle(AuthContext ctx)    {
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
