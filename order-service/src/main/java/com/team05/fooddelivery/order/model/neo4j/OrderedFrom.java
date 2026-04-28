package com.team05.fooddelivery.order.model.neo4j;

import java.time.LocalDateTime;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class OrderedFrom {

    @RelationshipId
    private Long id;

    private int orderCount;
    private LocalDateTime lastOrderDate;

    @TargetNode
    private RestaurantNode restaurant;

    public OrderedFrom(RestaurantNode restaurant) {
        this.restaurant = restaurant;
    }

    public Long getId() { return id; }

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) {this.lastOrderDate = lastOrderDate; }

    public RestaurantNode getRestaurant() { return restaurant; }
    public void setRestaurant(RestaurantNode restaurant) { this.restaurant = restaurant; }


}