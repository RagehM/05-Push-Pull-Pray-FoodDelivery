package com.team05.fooddelivery.checkout.config;

import com.team05.fooddelivery.checkout.security.CorrelationIdFilter;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * [S5-READ-DB] Centralised Feign configuration for checkout-service.
 *
 * <p>Provides:
 * <ul>
 *   <li>A logger level so we can see Feign calls in Loki.</li>
 *   <li>A {@link Retryer} that retries idempotent failures up to 3 times.</li>
 *   <li>A {@link Request.Options} bean to bound connect/read timeouts.</li>
 *   <li>A {@link RequestInterceptor} that forwards the inbound caller's JWT
 *       onto every outbound Feign call.</li>
 *   <li>A {@link RequestInterceptor} that propagates {@code X-Correlation-ID}
 *       from MDC (populated by {@link CorrelationIdFilter}) onto every
 *       outbound Feign call.</li>
 * </ul>
 *
 * <p>Error handling intentionally relies on Feign's default {@code ErrorDecoder}:
 * 5xx responses become {@code RetryableException} (handled by {@link Retryer}),
 * 4xx responses become {@code FeignException} variants (e.g.
 * {@code FeignException.NotFound}, {@code FeignException.Forbidden}) which the
 * service layer catches and converts to {@code ResponseStatusException}s.
 */
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        // BASIC = method + URL + response status + execution time. Cheap, useful in Loki.
        return Logger.Level.BASIC;
    }

    @Bean
    public Retryer feignRetryer() {
        // 3 attempts, exponential-ish backoff between 100ms and 1s.
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    /**
     * [S5-READ-DB] Forward the inbound caller's JWT onto every outbound Feign call.
     *
     * Without this, Spring Cloud OpenFeign builds a fresh HTTP request with no
     * Authorization header, so downstream services (user-service, order-service,
     * restaurant-service) reject the call with 401. The interceptor pulls the
     * Authorization header off the current servlet request and re-attaches it.
     *
     * Note: this only works when we're inside a Spring MVC request thread.
     * Background jobs that fire Feign calls would need a different strategy
     * (e.g. a dedicated service-to-service token).
     */
    @Bean
    public RequestInterceptor feignAuthForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return; // not in an HTTP request — skip silently
            }
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && !auth.isBlank()) {
                template.header(HttpHeaders.AUTHORIZATION, auth);
            }
        };
    }

    /**
     * [S5-READ-DB / Section 2.3] Propagate the inbound {@code X-Correlation-ID} onto every
     * outbound Feign call. The header is read from MDC, which is populated by
     * {@link CorrelationIdFilter} on the way in.
     *
     * If no correlation id is present (e.g. the call is fired from a background
     * job outside an HTTP request), the header is silently omitted — downstream
     * services will generate their own.
     */
    @Bean
    public RequestInterceptor feignCorrelationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null && !correlationId.isBlank()) {
                template.header(CorrelationIdFilter.HEADER, correlationId);
            }
        };
    }
}
