package com.team05.fooddelivery.delivery.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public class DelayedDeliveryDTO {

    private final Long deliveryId;
    private final String driverName;
    private final Long orderId;
    private final Double latitude;
    private final Double longitude;
    private final Double estimatedArrival;
    private final LocalDateTime updatedAt;

    private DelayedDeliveryDTO(Builder builder) {
        this.deliveryId = builder.deliveryId;
        this.driverName = builder.driverName;
        this.orderId = builder.orderId;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.estimatedArrival = builder.estimatedArrival;
        this.updatedAt = builder.updatedAt;
    }

    public Long getDeliveryId() { return deliveryId; }
    public String getDriverName() { return driverName; }
    public Long getOrderId() { return orderId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getEstimatedArrival() { return estimatedArrival; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @JsonCreator
    private DelayedDeliveryDTO(
            @JsonProperty("deliveryId") Long deliveryId,
            @JsonProperty("driverName") String driverName,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("estimatedArrival") Double estimatedArrival,
            @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.deliveryId = deliveryId;
        this.driverName = driverName;
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedArrival = estimatedArrival;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long deliveryId;
        private String driverName;
        private Long orderId;
        private Double latitude;
        private Double longitude;
        private Double estimatedArrival;
        private LocalDateTime updatedAt;

        public Builder deliveryId(Long deliveryId) {
            this.deliveryId = deliveryId;
            return this;
        }

        public Builder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder estimatedArrival(Double estimatedArrival) {
            this.estimatedArrival = estimatedArrival;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public DelayedDeliveryDTO build() {
            return new DelayedDeliveryDTO(this);
        }
    }
}