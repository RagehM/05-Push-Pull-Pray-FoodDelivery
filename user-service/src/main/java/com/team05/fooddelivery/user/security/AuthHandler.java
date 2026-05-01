package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;

public abstract class AuthHandler {
    protected AuthHandler nextHandler;
    AuthContext handle(AuthContext ctx)
    {
        return nextHandler.handle(ctx);
    }

    void setNext(AuthHandler next)
    {
        this.nextHandler = next;
    }
}
