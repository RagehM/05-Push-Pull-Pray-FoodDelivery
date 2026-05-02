package com.team05.fooddelivery.user.dto;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = UserOrderSummaryDTO.Builder.class)
public class UserOrderSummaryDTO{
    private Long userId;
    private String name;
    private Integer totalOrders;
    private Integer deliveredOrders;
    private Integer cancelledOrders;
    private Double totalSpent;
    private Double averageOrderAmount;

    private UserOrderSummaryDTO(Builder builder) {
        this.userId = builder.userId;
        this.name = builder.name;
        this.totalOrders = builder.totalOrders;
        this.deliveredOrders = builder.deliveredOrders;
        this.cancelledOrders = builder.cancelledOrders;
        this.totalSpent = builder.totalSpent;
        this.averageOrderAmount = builder.averageOrderAmount;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Integer getTotalOrders() { return totalOrders; }
    public Integer getDeliveredOrders() { return deliveredOrders; }
    public Integer getCancelledOrders() { return cancelledOrders; }
    public Double getTotalSpent() { return totalSpent; }
    public Double getAverageOrderAmount() { return averageOrderAmount; }


    public static Builder builder() {
        return new Builder();
    }
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Long userId;
        private String name;
        private Integer totalOrders;
        private Integer deliveredOrders;
        private Integer cancelledOrders;
        private Double totalSpent;
        private Double averageOrderAmount;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder totalOrders(Integer totalOrders) {
            this.totalOrders = totalOrders;
            return this;
        }
        public Builder deliveredOrders(Integer deliveredOrders) {
            this.deliveredOrders = deliveredOrders;
            return this;
        }
        public Builder cancelledOrders(Integer cancelledOrders) {
            this.cancelledOrders = cancelledOrders;
            return this;
        }
        public Builder totalSpent(Double totalSpent) {
            this.totalSpent = totalSpent;
            return this;
        }
        public Builder averageOrderAmount(Double averageOrderAmount) {
            this.averageOrderAmount = averageOrderAmount;
            return this;
        }
        public UserOrderSummaryDTO build() {
            return new UserOrderSummaryDTO(this);
        }
    }

}


