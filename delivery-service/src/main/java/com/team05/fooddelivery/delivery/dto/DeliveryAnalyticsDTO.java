package com.team05.fooddelivery.delivery.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team05.fooddelivery.delivery.enums.DeliveryStatus;

import java.util.Map;

public class DeliveryAnalyticsDTO {

    private final long totalDeliveries;
    private final double averageDeliveryTimeMinutes;
    private final double onTimeRate;
    private final Map<DeliveryStatus, Long> deliveriesByStatus;

    private DeliveryAnalyticsDTO(Builder builder) {
        this.totalDeliveries = builder.totalDeliveries;
        this.averageDeliveryTimeMinutes = builder.averageDeliveryTimeMinutes;
        this.onTimeRate = builder.onTimeRate;
        this.deliveriesByStatus = builder.deliveriesByStatus;
    }

    public long getTotalDeliveries() { return totalDeliveries; }
    public double getAverageDeliveryTimeMinutes() { return averageDeliveryTimeMinutes; }
    public double getOnTimeRate() { return onTimeRate; }
    public Map<DeliveryStatus, Long> getDeliveriesByStatus() { return deliveriesByStatus; }

    @JsonCreator
    private DeliveryAnalyticsDTO(
            @JsonProperty("totalDeliveries") long totalDeliveries,
            @JsonProperty("averageDeliveryTimeMinutes") double averageDeliveryTimeMinutes,
            @JsonProperty("onTimeRate") double onTimeRate,
            @JsonProperty("deliveriesByStatus") Map<DeliveryStatus, Long> deliveriesByStatus) {
        this.totalDeliveries = totalDeliveries;
        this.averageDeliveryTimeMinutes = averageDeliveryTimeMinutes;
        this.onTimeRate = onTimeRate;
        this.deliveriesByStatus = deliveriesByStatus;
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private long totalDeliveries;
        private double averageDeliveryTimeMinutes;
        private double onTimeRate;
        private Map<DeliveryStatus, Long> deliveriesByStatus;

        public Builder totalDeliveries(long totalDeliveries) {
            this.totalDeliveries = totalDeliveries;
            return this;
        }

        public Builder averageDeliveryTimeMinutes(double averageDeliveryTimeMinutes) {
            this.averageDeliveryTimeMinutes = averageDeliveryTimeMinutes;
            return this;
        }

        public Builder onTimeRate(double onTimeRate) {
            this.onTimeRate = onTimeRate;
            return this;
        }

        public Builder deliveriesByStatus(Map<DeliveryStatus, Long> deliveriesByStatus) {
            this.deliveriesByStatus = deliveriesByStatus;
            return this;
        }

        public DeliveryAnalyticsDTO build() {
            return new DeliveryAnalyticsDTO(this);
        }
    }
}