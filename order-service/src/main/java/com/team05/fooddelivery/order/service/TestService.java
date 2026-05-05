package com.team05.fooddelivery.order.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.team05.shared.model.mongo.OrderEvent;
import com.team05.fooddelivery.order.model.neo4j.*;
import com.team05.fooddelivery.order.repository.mongo.MongoOrderEventRepository;
import com.team05.fooddelivery.order.repository.neo4j.UserNodeRepository;

@Service
public class TestService {

    private final UserNodeRepository userNodeRepository;
    private final MongoOrderEventRepository eventRepository;


    public TestService(UserNodeRepository userNodeRepository, MongoOrderEventRepository eventRepository) {
        this.userNodeRepository = userNodeRepository;
        this.eventRepository = eventRepository;
    }

    public String testNeo4jCreation() {


        System.out.println("Testing Neo4j Creation...");

        RestaurantNode restaurant = new RestaurantNode("Pasta Palace", "Italian");
        System.out.println("Created RestaurantNode: " + restaurant.getName() + ", " + restaurant.getCuisineType());
        UserNode user = new UserNode("Alice");
        System.out.println("Created UserNode: " + user.getName());

        OrderedFrom relationship = new OrderedFrom(restaurant);
        System.out.println("Created OrderedFrom relationship between " + user.getName() + " and " + restaurant.getName());
        relationship.setOrderCount(5);
        relationship.setLastOrderDate(LocalDateTime.now());
        System.out.println("Set relationship properties: orderCount=" + relationship.getOrderCount() + ", lastOrderDate=" + relationship.getLastOrderDate());

        user.getOrderedFroms().add(relationship);

        System.out.println("Saving UserNode with relationship to Neo4j...");
        userNodeRepository.save(user);
        System.out.println("UserNode saved successfully with ID: " + user.getUserId());

        return userNodeRepository.findAll().toString();
    }

    public String testMongoCreation() {

        OrderEvent event = new OrderEvent(123L, OrderEvent.OrderEventActions.ORDER_CREATED, null);
        event.getDetails().put("customerName", "Alice");
        event.getDetails().put("totalAmount", 29.99);

        eventRepository.save(event);
        
        return eventRepository.findByOrderId(123L).toString();
    }
}