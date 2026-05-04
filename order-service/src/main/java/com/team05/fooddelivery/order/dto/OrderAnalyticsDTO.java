package com.team05.fooddelivery.order.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderAnalyticsDTO {
    private final Long totalOrders;
    private final Long deliveredOrders;
    private final Long cancelledOrders;
    private final Double totalRevenue;
    private final Double averageOrderAmount;
    private final Double deliveryRate;

    private OrderAnalyticsDTO(Builder builder) {
        this.totalOrders = builder.totalOrders;
        this.deliveredOrders = builder.deliveredOrders;
        this.cancelledOrders = builder.cancelledOrders;
        this.totalRevenue = builder.totalRevenue;
        this.averageOrderAmount = builder.averageOrderAmount;
        this.deliveryRate = builder.deliveryRate;
    }

    @JsonCreator
    private static OrderAnalyticsDTO fromJson(
            @JsonProperty("totalOrders") Long totalOrders,
            @JsonProperty("deliveredOrders") Long deliveredOrders,
            @JsonProperty("cancelledOrders") Long cancelledOrders,
            @JsonProperty("totalRevenue") Double totalRevenue,
            @JsonProperty("averageOrderAmount") Double averageOrderAmount,
            @JsonProperty("deliveryRate") Double deliveryRate) {
        return OrderAnalyticsDTO.builder()
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .averageOrderAmount(averageOrderAmount)
                .deliveryRate(deliveryRate)
                .build();
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public Long getDeliveredOrders() {
        return deliveredOrders;
    }

    public Long getCancelledOrders() {
        return cancelledOrders;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public Double getAverageOrderAmount() {
        return averageOrderAmount;
    }

    public Double getDeliveryRate() {
        return deliveryRate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long totalOrders;
        private Long deliveredOrders;
        private Long cancelledOrders;
        private Double totalRevenue;
        private Double averageOrderAmount;
        private Double deliveryRate;

        public Builder totalOrders(Long totalOrders) {
            this.totalOrders = totalOrders;
            return this;
        }

        public Builder deliveredOrders(Long deliveredOrders) {
            this.deliveredOrders = deliveredOrders;
            return this;
        }

        public Builder cancelledOrders(Long cancelledOrders) {
            this.cancelledOrders = cancelledOrders;
            return this;
        }

        public Builder totalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder averageOrderAmount(Double averageOrderAmount) {
            this.averageOrderAmount = averageOrderAmount;
            return this;
        }

        public Builder deliveryRate(Double deliveryRate) {
            this.deliveryRate = deliveryRate;
            return this;
        }

        public OrderAnalyticsDTO build() {
            return new OrderAnalyticsDTO(this);
        }
    }
}
