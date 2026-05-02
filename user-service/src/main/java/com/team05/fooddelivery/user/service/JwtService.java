package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.config.JwtConfigurationManager;
import com.team05.fooddelivery.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final JwtConfigurationManager jwtConfig;

    public JwtService() {
    this.jwtConfig=JwtConfigurationManager.getInstance();
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole())
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] bytes = Decoders.BASE64.decode(JwtConfigurationManager.getInstance().getSecret());
        return Keys.hmacShaKeyFor(bytes);
    }
}
