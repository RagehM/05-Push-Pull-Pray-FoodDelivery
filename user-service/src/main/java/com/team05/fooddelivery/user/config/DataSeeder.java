package com.team05.fooddelivery.user.config;

import com.team05.fooddelivery.user.enums.UserRole;
import com.team05.fooddelivery.user.model.User;
import com.team05.fooddelivery.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DataSeeder {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean seedAdminAccount() {
        String adminEmail = "admin@guc.edu.eg";

        if (userRepository.existsByEmail(adminEmail)) {
            return false;
        }

        User admin = new User();

        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setName("Admin");

        userRepository.save(admin);

        return true;
    }
}
