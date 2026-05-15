package com.team05.fooddelivery.checkout.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * [S5-READ-DB / Section 2.3] Populates MDC with a correlation id for the duration of
 * every HTTP request so:
 * <ul>
 *   <li>logback-spring.xml can include {@code correlationId=...} in every log line
 *       (filterable in Loki with {@code | logfmt | correlationId="..."}).</li>
 *   <li>The {@code feignCorrelationIdInterceptor} can attach the same id as
 *       {@code X-Correlation-ID} on every outbound Feign call, so the entire
 *       chain across services shares one id.</li>
 * </ul>
 *
 * <p>Behaviour:
 * <ul>
 *   <li>If the inbound request carries {@code X-Correlation-ID}, it is reused.</li>
 *   <li>If not (the request entered the system here), a fresh UUID is generated.</li>
 *   <li>The id is also written to the response so the caller can correlate.</li>
 *   <li>MDC is always cleared in {@code finally} so Tomcat thread-pool reuse
 *       doesn't leak the id to a subsequent unrelated request.</li>
 * </ul>
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so the correlation id is already
 * in MDC by the time security filters and controllers log anything.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP header used both inbound and outbound. */
    public static final String HEADER = "X-Correlation-ID";

    /** MDC key referenced by logback-spring.xml and the Feign interceptor. */
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        try {
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(HEADER, correlationId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
