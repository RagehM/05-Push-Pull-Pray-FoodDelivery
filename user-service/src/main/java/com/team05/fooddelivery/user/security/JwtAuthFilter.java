package com.team05.fooddelivery.user.security;

import com.team05.fooddelivery.user.dto.AuthContext;
import com.team05.fooddelivery.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {


        try
        {
            AuthHandler tokenExtractionHandler = new TokenExtractionHandler();
            AuthHandler signatureValidationHandler = new SignatureValidationHandler(jwtService);
            AuthHandler userLoaderHandler = new UserLoaderHandler(jwtService, userDetailsService);
            AuthHandler roleAuthorizationHandler = new RoleAuthorizationHandler();


            tokenExtractionHandler.setNext(signatureValidationHandler);
            signatureValidationHandler.setNext(userLoaderHandler);
            userLoaderHandler.setNext(roleAuthorizationHandler);

            AuthContext ctx = new AuthContext(request,null,null,null);
            ctx = tokenExtractionHandler.handle(ctx);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    ctx.user(),
                    null,
                    ctx.user().getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
        catch (Exception e) {
        }


    }
}
