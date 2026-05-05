package com.team05.fooddelivery.restaurant.dto;

// [S2-F12] Response DTO for the Restaurant Performance Dashboard endpoint.
// Builder pattern — Section 3.5 (5+ fields)
public class RestaurantDashboardDTO {

    private Long restaurantId;
    private String name;
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderValue;
    private Long activeMenuItems;

    private RestaurantDashboardDTO(Builder builder) {
        this.restaurantId = builder.restaurantId;
        this.name = builder.name;
        this.totalOrders = builder.totalOrders;
        this.totalRevenue = builder.totalRevenue;
        this.averageOrderValue = builder.averageOrderValue;
        this.activeMenuItems = builder.activeMenuItems;
    }

    public RestaurantDashboardDTO() {}

    public Long getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public Long getTotalOrders() { return totalOrders; }
    public Double getTotalRevenue() { return totalRevenue; }
    public Double getAverageOrderValue() { return averageOrderValue; }
    public Long getActiveMenuItems() { return activeMenuItems; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long restaurantId;
        private String name;
        private Long totalOrders;
        private Double totalRevenue;
        private Double averageOrderValue;
        private Long activeMenuItems;

        public Builder restaurantId(Long restaurantId) { this.restaurantId = restaurantId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalOrders(Long totalOrders) { this.totalOrders = totalOrders; return this; }
        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder averageOrderValue(Double averageOrderValue) { this.averageOrderValue = averageOrderValue; return this; }
        public Builder activeMenuItems(Long activeMenuItems) { this.activeMenuItems = activeMenuItems; return this; }

        public RestaurantDashboardDTO build() {
            return new RestaurantDashboardDTO(this);
        }
    }
}