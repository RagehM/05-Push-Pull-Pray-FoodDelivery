package com.team05.fooddelivery.user.service;

import com.team05.fooddelivery.user.config.JwtConfigurationManager;
import com.team05.fooddelivery.user.dto.AuthResponse;
import com.team05.fooddelivery.user.dto.LoginRequest;
import com.team05.fooddelivery.user.dto.RegisterRequest;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import com.team05.fooddelivery.user.repository.mongo.AuthEventRepository;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.observer.EntityObserver;
import com.team05.shared.observer.MongoEventLogger;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfigurationManager jwtConfig;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final AuthEventRepository authEventRepository;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService,AuthEventRepository authEventRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtConfig = JwtConfigurationManager.getInstance();
        this.authEventRepository = authEventRepository;
        this.observers.add(
                new MongoEventLogger<>(this.authEventRepository, MongoEvent.EventType.AUTH)
        );
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }


    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(401), "invalid credentials"));

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401), "invalid credentials");
        }

        String token = jwtService.generateToken(user);


        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", user.getId());
        authEvent.put("action", "LOGGED_IN");

        notifyObservers("LOGGED_IN", authEvent);

        return new AuthResponse(token, jwtConfig.getExpiration());
    }

    public AuthResponse register(RegisterRequest request) {
        if(request.name() == null || request.name().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "name is required");
        }
        if(request.password() == null || request.password().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "password is required");
        }
        if(request.email() == null || request.email().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "email is required");
        }
        if(request.phone() == null || request.phone().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "phone is required");
        }

        if(userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(409), "email is taken");
        }
        if(userRepository.existsByPhone(request.phone())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(409), "phone is taken");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(com.team05.fooddelivery.user.enums.UserRole.CUSTOMER);
        user.setStatus(com.team05.fooddelivery.user.enums.UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        Map<String, Object> authEvent = new HashMap<>();
        authEvent.put("userId", user.getId());
        authEvent.put("action", "REGISTERED");

        notifyObservers("REGISTERED", authEvent);

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, jwtConfig.getExpiration());
    }
}