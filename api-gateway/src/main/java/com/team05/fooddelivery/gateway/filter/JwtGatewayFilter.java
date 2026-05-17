package com.team05.fooddelivery.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private final SecretKey signingKey;

    public JwtGatewayFilter(@Value("${JWT_SECRET}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);

        if (path.startsWith("/api/auth/")) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(CORRELATION_ID_HEADER, finalCorrelationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("uid", Long.class);
            String role = claims.get("role", String.class);
            if (userId == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(USER_ID_HEADER, String.valueOf(userId))
                    .header(USER_ROLE_HEADER, role != null ? role : "")
                    .header(CORRELATION_ID_HEADER, finalCorrelationId)
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
