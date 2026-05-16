package com.team05.fooddelivery.checkout;

import com.team05.fooddelivery.contracts.feign.OrderServiceClient;
import com.team05.fooddelivery.contracts.feign.RestaurantServiceClient;
import com.team05.fooddelivery.contracts.feign.UserServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableCaching
@EnableFeignClients(clients = {UserServiceClient.class, OrderServiceClient.class, RestaurantServiceClient.class})
public class CheckoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }

}
