package com.team05.fooddelivery.checkout.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team05.fooddelivery.checkout.config.JwtConfigurationManager;
import com.team05.fooddelivery.checkout.security.AuthHandler;
import com.team05.fooddelivery.checkout.security.UserLoaderHandler;
import com.team05.fooddelivery.checkout.service.JwtService;
import com.team05.shared.dto.AuthContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtConfigurationManager jwtConfigurationManager;

    /** Endpoint patterns that require a specific role to be enforced at the filter level. */
    private static final Map<String, Map<String, String>> ROLE_REQUIREMENTS = new LinkedHashMap<>();

    static {
        Map<String, String> putRoles = new LinkedHashMap<>();
        putRoles.put("/api/users/*/role", "ADMIN");
        ROLE_REQUIREMENTS.put("PUT", putRoles);
    }

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/users/health",
            "/api/seed"
    );

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.jwtConfigurationManager = JwtConfigurationManager.getInstance();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private String resolveRequiredRole(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        Map<String, String> methodMap = ROLE_REQUIREMENTS.get(method);
        if (methodMap != null) {
            for (Map.Entry<String, String> entry : methodMap.entrySet()) {
                if (pathMatcher.match(entry.getKey(), path)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            AuthHandler tokenExtractionHandler = new TokenExtractionHandler();
            AuthHandler signatureValidationHandler = new SignatureValidationHandler(jwtService);
            AuthHandler userLoaderHandler = new UserLoaderHandler(jwtService);
            AuthHandler roleAuthorizationHandler = new RoleAuthorizationHandler(jwtService,jwtConfigurationManager);

            tokenExtractionHandler.setNext(signatureValidationHandler);
            signatureValidationHandler.setNext(userLoaderHandler);
            userLoaderHandler.setNext(roleAuthorizationHandler);

            String requiredRole = resolveRequiredRole(request);

            AuthContext ctx = tokenExtractionHandler.handle(new AuthContext(request, null, null, requiredRole));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    null,
                    null,
                    null);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);

        } catch (ResponseStatusException e) {
            String reason = e.getReason() != null ? e.getReason() : e.getMessage();
            writeErrorResponse(response, e.getStatusCode().value(), reason);

        } catch (Exception e) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized: " + e.getMessage());
        }
    }
}
