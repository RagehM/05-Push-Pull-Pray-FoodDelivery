package com.team05.fooddelivery.checkout.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = CuisineRevenueDTO.Builder.class)
public class CuisineRevenueDTO {

    private String cuisineType;
    private Double foodRevenue;
    private Double deliveryFeeRevenue;
    private Double totalRevenue;
    private Long orderCount;

    private CuisineRevenueDTO(Builder builder) {
        this.cuisineType = builder.cuisineType;
        this.foodRevenue = builder.foodRevenue;
        this.deliveryFeeRevenue = builder.deliveryFeeRevenue;
        this.totalRevenue = builder.totalRevenue;
        this.orderCount = builder.orderCount;
    }

    public String getCuisineType() { return cuisineType; }
    public Double getFoodRevenue() { return foodRevenue; }
    public Double getDeliveryFeeRevenue() { return deliveryFeeRevenue; }
    public Double getTotalRevenue() { return totalRevenue; }
    public Long getOrderCount() { return orderCount; }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String cuisineType;
        private Double foodRevenue;
        private Double deliveryFeeRevenue;
        private Double totalRevenue;
        private Long orderCount;

        public Builder cuisineType(String cuisineType) { this.cuisineType = cuisineType; return this; }
        public Builder foodRevenue(Double foodRevenue) { this.foodRevenue = foodRevenue; return this; }
        public Builder deliveryFeeRevenue(Double deliveryFeeRevenue) { this.deliveryFeeRevenue = deliveryFeeRevenue; return this; }
        public Builder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public Builder orderCount(Long orderCount) { this.orderCount = orderCount; return this; }

        public CuisineRevenueDTO build() {
            return new CuisineRevenueDTO(this);
        }
    }
}
