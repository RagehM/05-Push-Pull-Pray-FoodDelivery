package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.service.DataSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SeedController {

    private static final Logger log = LoggerFactory.getLogger(SeedController.class);
    private final DataSeeder dataSeeder;

    public SeedController(DataSeeder dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    @GetMapping("/api/seed")
    public ResponseEntity<Map<String, String>> seed() {
        log.info("Received {} {}", "GET", "/api/seed");
        boolean adminSeeded = dataSeeder.seedAdminAccount();
        ResponseEntity<Map<String, String>> result = ResponseEntity.ok(Map.of(
                "admin", adminSeeded ? "SEEDED" : "SKIPPED, IT IS ALREADY SEEDED"));
        log.info("Returning {} for {} {}", 200, "GET", "/api/seed");
        return result;
    }
}