package com.team05.fooddelivery.order.config;

import com.team05.fooddelivery.order.security.CorrelationIdFilter;
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

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    @Bean
    public RequestInterceptor feignAuthForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && !auth.isBlank()) {
                template.header(HttpHeaders.AUTHORIZATION, auth);
            }
            String userId = req.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                template.header("X-User-Id", userId);
            }
            String userRole = req.getHeader("X-User-Role");
            if (userRole != null && !userRole.isBlank()) {
                template.header("X-User-Role", userRole);
            }
        };
    }

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
