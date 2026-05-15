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
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        if (path.startsWith("/api/auth/")) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Correlation-ID", finalCorrelationId)
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
            String secret = System.getenv("JWT_SECRET");
            Claims claims = Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-Correlation-ID", finalCorrelationId)
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
