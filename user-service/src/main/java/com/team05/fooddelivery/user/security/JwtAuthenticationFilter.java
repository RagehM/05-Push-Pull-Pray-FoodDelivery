package com.team05.fooddelivery.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team05.fooddelivery.user.dto.AuthContext;
import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.service.JwtService;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, Map<String, UserRole>> ROLE_REQUIREMENTS = new LinkedHashMap<>();

    static {
        Map<String, UserRole> putRoles = new LinkedHashMap<>();
        putRoles.put("/api/users/*/role", UserRole.ADMIN);
        ROLE_REQUIREMENTS.put("PUT", putRoles);
    }

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/users/health",
            "/api/seed",
            "/actuator/**"
    );

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private UserRole resolveRequiredRole(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        Map<String, UserRole> methodMap = ROLE_REQUIREMENTS.get(method);
        if (methodMap != null) {
            for (Map.Entry<String, UserRole> entry : methodMap.entrySet()) {
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
            AuthHandler head = new TokenExtractionHandler();
            AuthHandler signatureValidationHandler = new SignatureValidationHandler(jwtService);
            AuthHandler userLoaderHandler = new UserLoaderHandler(jwtService, userDetailsService);
            AuthHandler roleAuthorizationHandler = new RoleAuthorizationHandler();


            head.setNext(signatureValidationHandler);
            signatureValidationHandler.setNext(userLoaderHandler);
            userLoaderHandler.setNext(roleAuthorizationHandler);

            UserRole requiredRole = resolveRequiredRole(request);

            AuthContext ctx = head.handle(new AuthContext(request, null, null, requiredRole));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    ctx.user(),
                    null,
                    ctx.user().getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
        catch (ResponseStatusException e) {
            String reason = e.getReason() != null ? e.getReason() : e.getMessage();
            writeErrorResponse(response, e.getStatusCode().value(), reason);

        } catch (Exception e) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized: " + e.getMessage());
        }
    }
}
