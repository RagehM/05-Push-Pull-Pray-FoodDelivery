package com.team05.fooddelivery.user.controller;


import com.team05.fooddelivery.user.dto.AuthResponse;
import com.team05.fooddelivery.user.dto.LoginRequest;
import com.team05.fooddelivery.user.dto.RegisterRequest;
import com.team05.fooddelivery.user.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("Received {} {}", "POST", "/api/auth/register");
        AuthResponse response = authService.register(request);
        log.info("Returning {} for {} {}", 201, "POST", "/api/auth/register");
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Received {} {}", "POST", "/api/auth/login");
        AuthResponse response = authService.login(request);
        log.info("Returning {} for {} {}", 200, "POST", "/api/auth/login");
        return ResponseEntity.ok(response);
    }
}