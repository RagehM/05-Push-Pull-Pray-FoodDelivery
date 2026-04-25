package com.team05.fooddelivery.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching 
public class R {

    public static void main(String[] args) {
        SpringApplication.run(R.class, args);
    }

}
