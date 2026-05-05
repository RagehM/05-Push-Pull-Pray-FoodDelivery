package com.team05.fooddelivery.order.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestaurantRecommendationDTO {
    private final Long id;
    private final String name;
    private final String cuisineType;

    private RestaurantRecommendationDTO(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.cuisineType = builder.cuisineType;
    }

    @JsonCreator
    private RestaurantRecommendationDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("cuisineType") String cuisineType
    ) {
        this.id = id;
        this.name = name;
        this.cuisineType = cuisineType;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCuisineType() { return cuisineType; }

    public static class Builder {
        private Long id;
        private String name;
        private String cuisineType;

        public Builder id(Long id) {
            this.id = id;
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

        public RestaurantRecommendationDTO build() {
            return new RestaurantRecommendationDTO(this);
        }
    }
}