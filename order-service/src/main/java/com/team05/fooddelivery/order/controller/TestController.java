package com.team05.fooddelivery.order.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team05.fooddelivery.order.service.TestService;

import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/api/orders")
public class TestController {

    private final TestService testService;
    public TestController(TestService testService) {
        this.testService = testService;
    }   

    @PostMapping("/neo")
    public String testNeo() {

        return testService.testNeo4jCreation();
        
    }

    @PostMapping("mongo")
    public String testMongo() {
        return testService.testMongoCreation();
    }
    
    
}
