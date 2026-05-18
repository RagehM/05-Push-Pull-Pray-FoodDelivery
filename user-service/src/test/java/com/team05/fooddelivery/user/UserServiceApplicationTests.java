package com.team05.fooddelivery.user;

import com.team05.fooddelivery.user.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class UserServiceApplicationTests {

    /**
     * Mock JwtService so that JwtConfigurationManager.getInstance() is never called
     * and the JWT_SECRET / JWT_EXPIRATION environment variables are not required at test time.
     */
    @MockitoBean
    private JwtService jwtService;

    @Test
    void contextLoads() {
    }

}
