package com.team05.fooddelivery.checkout.config;

import feign.Logger;
import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

/**
 * [S5-READ-DB] Centralised Feign configuration for checkout-service.
 *
 * <p>Provides:
 * <ul>
 *   <li>A logger level so we can see Feign calls in Loki.</li>
 *   <li>A {@link Retryer} that retries idempotent failures up to 3 times.</li>
 *   <li>An {@link ErrorDecoder} that translates upstream HTTP failures into
 *       {@link ResponseStatusException} so they propagate cleanly through
 *       the global exception handler.</li>
 *   <li>A {@link Request.Options} bean to bound connect/read timeouts.</li>
 * </ul>
 */
@Configuration
public class FeignConfig {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FeignConfig.class);

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
     * Translate upstream HTTP errors into {@link ResponseStatusException}s
     * so the checkout-service's {@code GlobalExceptionHandler} can render them
     * with the same shape used everywhere else.
     *
     * 5xx responses are wrapped in a {@link RetryableException} so the
     * {@link Retryer} above can take another shot — 4xx is treated as a
     * client-side problem and surfaced immediately.
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            String reason = "Upstream call " + methodKey + " failed (status=" + status + ")";
            log.warn("Feign error: methodKey={} status={} reason={}", methodKey, status, response.reason());

            if (status >= 500) {
                // retryable
                return new RetryableException(
                        status,
                        reason,
                        response.request() == null ? null : response.request().httpMethod(),
                        (Long) null,
                        response.request()
                );
            }
            if (status == 404) {
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Upstream resource not found: " + methodKey);
            }
            if (status >= 400) {
                return new ResponseStatusException(HttpStatus.valueOf(status), reason);
            }
            // Fallback (3xx etc.) — surface as 502 so callers don't think the call succeeded
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason);
        };
    }
}
