package com.team05.fooddelivery.checkout.security;

import com.team05.fooddelivery.checkout.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtConfigurationManager jwtConfig;

    public JwtAuthFilter() {
        this.jwtConfig = JwtConfigurationManager.getInstance();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);
            Claims claims = Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecret())))
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            // Invalid token — Spring Security will return 401
        }

        chain.doFilter(request, response);
    }
}

