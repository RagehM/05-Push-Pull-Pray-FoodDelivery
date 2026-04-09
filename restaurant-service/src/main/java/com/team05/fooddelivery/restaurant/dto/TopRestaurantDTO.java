// for s2-f6
package com.team05.fooddelivery.restaurant.dto;

public class TopRestaurantDTO {

    private Long restaurantId;
    private String name;
    private Double rating;
    private Long totalOrders;

    public TopRestaurantDTO(Long restaurantId, String name, Double rating, Long totalOrders) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.rating = rating;
        this.totalOrders = totalOrders;
    }

    public Long getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public Double getRating() { return rating; }
    public Long getTotalOrders() { return totalOrders; }
}