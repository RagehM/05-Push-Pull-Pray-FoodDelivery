package com.team05.fooddelivery.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import com.team05.fooddelivery.contracts.feign.OrderServiceClient;
//s2-f11
import com.team05.fooddelivery.restaurant.repository.elasticsearch.RestaurantSearchRepository;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableCaching
//s2-f11
@EnableElasticsearchRepositories(basePackageClasses = RestaurantSearchRepository.class)
@EnableFeignClients(clients = {OrderServiceClient.class})
public class RestaurantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantServiceApplication.class, args);
    }

}
