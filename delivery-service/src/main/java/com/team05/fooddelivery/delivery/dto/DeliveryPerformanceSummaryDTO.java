package com.team05.fooddelivery.delivery.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public class DeliveryPerformanceSummaryDTO {

    private String driverName;
    private long totalDeliveries;
    private double averageSpeed;
    private double maxSpeed;
    private LocalDateTime firstDelivery;
    private LocalDateTime lastDelivery;

    public DeliveryPerformanceSummaryDTO() {}

    public DeliveryPerformanceSummaryDTO(Builder builder) {
        this.driverName = builder.driverName;
        this.totalDeliveries = builder.totalDeliveries;
        this.averageSpeed = builder.averageSpeed;
        this.maxSpeed = builder.maxSpeed;
        this.firstDelivery = builder.firstDelivery;
        this.lastDelivery = builder.lastDelivery;
    }

    public String getDriverName() {
        return driverName;
    }

    public long getTotalDeliveries() {
        return totalDeliveries;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public LocalDateTime getFirstDelivery() {
        return firstDelivery;
    }

    public LocalDateTime getLastDelivery() {
        return lastDelivery;
    }

    @JsonCreator
    private DeliveryPerformanceSummaryDTO(
            @JsonProperty("driverName") String driverName,
            @JsonProperty("totalDeliveries") long totalDeliveries,
            @JsonProperty("orderId") Long averageSpeed,
            @JsonProperty("latitude") Double maxSpeed,
            @JsonProperty("longitude") LocalDateTime firstDelivery,
            @JsonProperty("distanceKm") LocalDateTime lastDelivery) {
        this.driverName = driverName;
        this.totalDeliveries = totalDeliveries;
        this.averageSpeed = averageSpeed;
        this.maxSpeed = maxSpeed;
        this.firstDelivery = firstDelivery;
        this.lastDelivery = lastDelivery;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String driverName;
        private long totalDeliveries;
        private double averageSpeed;
        private double maxSpeed;
        private LocalDateTime firstDelivery;
        private LocalDateTime lastDelivery;

        public Builder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder totalDeliveries(long totalDeliveries) {
            this.totalDeliveries = totalDeliveries;
            return this;
        }

        public Builder averageSpeed(double averageSpeed) {
            this.averageSpeed = averageSpeed;
            return this;
        }

        public Builder maxSpeed(double maxSpeed) {
            this.maxSpeed = maxSpeed;
            return this;
        }

        public Builder firstDelivery(LocalDateTime firstDelivery) {
            this.firstDelivery = firstDelivery;
            return this;
        }

        public Builder lastDelivery(LocalDateTime lastDelivery) {
            this.lastDelivery = lastDelivery;
            return this;
        }

        public DeliveryPerformanceSummaryDTO build() {
            return new DeliveryPerformanceSummaryDTO(this);
        }
    }
}