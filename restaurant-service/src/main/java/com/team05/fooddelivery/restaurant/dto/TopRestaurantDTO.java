package com.team05.fooddelivery.restaurant.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

// [S2-F6] Response DTO for the top-rated restaurants report endpoint.
// Builder pattern — Section 3.5
@JsonDeserialize(builder = TopRestaurantDTO.Builder.class)
public class TopRestaurantDTO {

    private Long restaurantId;
    private String name;
    private Double rating;
    private Long totalOrders;

    private TopRestaurantDTO(Builder builder) {
        this.restaurantId = builder.restaurantId;
        this.name = builder.name;
        this.rating = builder.rating;
        this.totalOrders = builder.totalOrders;
    }

    public Long getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public Double getRating() { return rating; }
    public Long getTotalOrders() { return totalOrders; }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Long restaurantId;
        private String name;
        private Double rating;
        private Long totalOrders;

        public Builder restaurantId(Long restaurantId) { this.restaurantId = restaurantId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder rating(Double rating) { this.rating = rating; return this; }
        public Builder totalOrders(Long totalOrders) { this.totalOrders = totalOrders; return this; }

        public TopRestaurantDTO build() {
            return new TopRestaurantDTO(this);
        }
    }
}