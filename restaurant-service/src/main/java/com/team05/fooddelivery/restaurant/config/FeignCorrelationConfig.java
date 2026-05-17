package com.team05.fooddelivery.restaurant.config;

import com.team05.fooddelivery.restaurant.security.CorrelationIdFilter;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// traceability: This configuration ensures that the correlation ID is included in all outgoing Feign client requests, allowing for better traceability across microservices.
// When a request is made to the restaurant service, the correlation ID is extracted from the MDC (Mapped Diagnostic Context) and added as a header to the outgoing Feign requests.
// This allows other services that receive these requests to log the correlation ID and trace the flow of requests across different services in the system, making it easier to debug and monitor the interactions between services.
// Feign is a declarative HTTP client developed by Netflix that simplifies the process of making HTTP requests to other services. 
// It allows developers to define interfaces for remote services and automatically generates the necessary code to make HTTP calls, handle responses, and manage errors. 
// By using Feign, developers can easily integrate with other microservices in a clean and efficient way, reducing boilerplate code and improving maintainability.
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