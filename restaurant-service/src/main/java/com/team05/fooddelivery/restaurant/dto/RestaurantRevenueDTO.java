package com.team05.fooddelivery.restaurant.dto;

public class RestaurantRevenueDTO {

    private Long restaurantId;
    private String name;
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderAmount;

    public RestaurantRevenueDTO(Long restaurantId, String name, Long totalOrders, Double totalRevenue,
            Double averageOrderAmount) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderAmount = averageOrderAmount;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getAverageOrderAmount() {
        return averageOrderAmount;
    }

    public void setAverageOrderAmount(Double averageOrderAmount) {
        this.averageOrderAmount = averageOrderAmount;
    }
}