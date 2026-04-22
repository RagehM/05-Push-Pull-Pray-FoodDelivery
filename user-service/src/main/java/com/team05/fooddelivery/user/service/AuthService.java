package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.config.JwtConfig;
import com.team05.fooddelivery.user.dto.AuthResponse;
import com.team05.fooddelivery.user.dto.LoginRequest;
import com.team05.fooddelivery.user.dto.RegisterRequest;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService, JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }


    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(401), "invalid credentials"));

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401), "invalid credentials");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, jwtConfig.getExpiration());
    }
}