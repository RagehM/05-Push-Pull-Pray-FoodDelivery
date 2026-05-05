package com.team05.fooddelivery.order.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Restaurant")
public class RestaurantNode {
    @Id
    private Long restaurantId;

    private String name;

    private String cuisineType;

    public RestaurantNode() {
    }

    public RestaurantNode(String name, String cuisineType) {
        this.name = name;
        this.cuisineType = cuisineType;
    }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCuisineType() { return cuisineType; }
    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
}
