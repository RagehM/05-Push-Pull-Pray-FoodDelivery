package com.team05.fooddelivery.restaurant.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

// [S2-F3] Response DTO for the revenue summary endpoint.
// Builder pattern — Section 3.5
@JsonDeserialize(builder = RestaurantRevenueDTO.Builder.class)
public class RestaurantRevenueDTO {

    private Long restaurantId;
    private String name;
    private Long totalOrders;
    private Double totalRevenue;
    private Double averageOrderAmount;

    private RestaurantRevenueDTO(Builder builder) {
        this.restaurantId = builder.restaurantId;
        this.name = builder.name;
        this.totalOrders = builder.totalOrders;
        this.totalRevenue = builder.totalRevenue;
        this.averageOrderAmount = builder.averageOrderAmount;
    }

    public Long getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public Long getTotalOrders() { return totalOrders; }
    public Double getTotalRevenue() { return totalRevenue; }
    public Double getAverageOrderAmount() { return averageOrderAmount; }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
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
            return new RestaurantRevenueDTO(this);
        }
    }
}