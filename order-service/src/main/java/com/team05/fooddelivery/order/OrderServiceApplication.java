package com.team05.fooddelivery.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.team05.fooddelivery.contracts.feign.DeliveryServiceClient;
import com.team05.fooddelivery.contracts.feign.RestaurantServiceClient;
import com.team05.fooddelivery.contracts.feign.UserServiceClient;


// Listens to Events: delivery.created, payment.initiated, payment.completed, payment.failed, payment.refunded

@SpringBootApplication
@EnableCaching
@EnableFeignClients(clients = {UserServiceClient.class, DeliveryServiceClient.class, RestaurantServiceClient.class})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
