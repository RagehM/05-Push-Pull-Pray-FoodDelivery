package com.team05.fooddelivery.delivery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class HealthController {

    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }
}
