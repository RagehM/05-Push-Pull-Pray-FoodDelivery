package com.team05.fooddelivery.restaurant.security;

import com.team05.shared.dto.AuthContext;

public abstract class AuthHandler {
    protected AuthHandler nextHandler;

    public AuthContext handle(AuthContext ctx) {
        if (nextHandler != null) {
            return nextHandler.handle(ctx);
        }
        return ctx;
    }

    public void setNext(AuthHandler next) {
        this.nextHandler = next;
    }
}
