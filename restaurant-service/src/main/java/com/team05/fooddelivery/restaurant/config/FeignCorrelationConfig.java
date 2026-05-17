package com.team05.fooddelivery.restaurant.config;

import com.team05.fooddelivery.restaurant.security.CorrelationIdFilter;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (correlationId != null) {
                template.header(CorrelationIdFilter.HEADER, correlationId);
            }
        };
    }
}
