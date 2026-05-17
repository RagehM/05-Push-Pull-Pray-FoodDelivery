package com.team05.fooddelivery.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/health")
    public String healthCheck() {
        log.info("Received {} {}", "GET", "/api/users/health");
        String result = "OK";
        log.info("Returning {} for {} {}", 200, "GET", "/api/users/health");
        return result;
    }
}