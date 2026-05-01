package com.team05.fooddelivery.checkout.config;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private JwtConfigurationManager() {
        this.secret = System.getenv("JWT_SECRET");

    }

    public static synchronized JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public String getSecret() {
        return secret;
    }
}