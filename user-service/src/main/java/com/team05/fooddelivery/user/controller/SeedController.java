package com.team05.fooddelivery.user.controller;

import com.team05.fooddelivery.user.config.DataSeeder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SeedController {

    private final DataSeeder dataSeeder;

    public SeedController(DataSeeder dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    @GetMapping("/api/seed")
    public ResponseEntity<Map<String, String>> seed() {
        boolean adminSeeded = dataSeeder.seedAdminAccount();
        return ResponseEntity.ok(Map.of(
                "admin", adminSeeded ? "SEEDED" : "SKIPPED, IT IS ALREADY SEEDED"));
    }
}
