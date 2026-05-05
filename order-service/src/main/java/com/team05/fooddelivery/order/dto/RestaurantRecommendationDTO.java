package com.team05.fooddelivery.order.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestaurantRecommendationDTO {

    private final Long restaurantId;
    private final String name;
    private final String cuisineType;
    private final Long score;

    @JsonCreator
    public RestaurantRecommendationDTO(
            @JsonProperty("restaurantId") Long restaurantId,
            @JsonProperty("name") String name,
            @JsonProperty("cuisineType") String cuisineType,
            @JsonProperty("score") Long score
    ) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.cuisineType = cuisineType;
        this.score = score;
    }

    private RestaurantRecommendationDTO(Builder builder) {
        this.restaurantId = builder.restaurantId;
        this.name = builder.name;
        this.cuisineType = builder.cuisineType;
        this.score = builder.score;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Long getId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public Long getScore() {
        return score;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long restaurantId;
        private String name;
        private String cuisineType;
        private Long score;

        public Builder restaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
            return this;
        }

        public Builder id(Long id) {
            this.restaurantId = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder cuisineType(String cuisineType) {
            this.cuisineType = cuisineType;
            return this;
        }

        public Builder score(Long score) {
            this.score = score;
            return this;
        }

        public RestaurantRecommendationDTO build() {
            return new RestaurantRecommendationDTO(this);
        }
    }
}