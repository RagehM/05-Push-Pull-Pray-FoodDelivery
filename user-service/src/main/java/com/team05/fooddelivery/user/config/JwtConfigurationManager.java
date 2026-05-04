package com.team05.fooddelivery.user.config;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expiration;

    private JwtConfigurationManager() {
        this.secret = System.getenv("JWT_SECRET") ;
        String expirationEnv = System.getenv("JWT_EXPIRATION");
        this.expiration = Long.parseLong(expirationEnv);
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

    public long getExpiration() {
        return expiration;
    }
}