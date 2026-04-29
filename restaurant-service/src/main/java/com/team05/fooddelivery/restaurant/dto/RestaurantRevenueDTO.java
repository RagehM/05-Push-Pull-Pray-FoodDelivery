package com.team05.fooddelivery.restaurant.dto;

// [S2-F3] Response DTO for the revenue summary endpoint.
// Holds aggregated order data (total orders, total revenue, average order amount) for a restaurant within a date range.
// Builder pattern added — Section 3.5
public class RestaurantRevenueDTO {

    private Long restaurantId;
    private String name;
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderAmount;

    private RestaurantRevenueDTO() {}

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    public Double getAverageOrderAmount() { return averageOrderAmount; }
    public void setAverageOrderAmount(Double averageOrderAmount) { this.averageOrderAmount = averageOrderAmount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long restaurantId;
        private String name;
        private Long totalOrders;
        private Double totalRevenue;
        private Double averageOrderAmount;

        public Builder restaurantId(Long restaurantId) { this.restaurantId = restaurantId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder totalOrders(Long totalOrders) { this.totalOrders = totalOrders; return this; }
        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder averageOrderAmount(Double averageOrderAmount) { this.averageOrderAmount = averageOrderAmount; return this; }

        public RestaurantRevenueDTO build() {
            RestaurantRevenueDTO dto = new RestaurantRevenueDTO();
            dto.restaurantId = this.restaurantId;
            dto.name = this.name;
            dto.totalOrders = this.totalOrders;
            dto.totalRevenue = this.totalRevenue;
            dto.averageOrderAmount = this.averageOrderAmount;
            return dto;
        }
    }
}