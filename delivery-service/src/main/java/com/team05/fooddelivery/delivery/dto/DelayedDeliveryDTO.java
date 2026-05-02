package com.team05.fooddelivery.delivery.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.LocalDateTime;

@JsonDeserialize(builder = DelayedDeliveryDTO.Builder.class)
public class DelayedDeliveryDTO {

    private Long deliveryId;
    private String driverName;
    private Long orderId;
    private Double latitude;
    private Double longitude;
    private Double estimatedArrival;
    private LocalDateTime updatedAt;

    public DelayedDeliveryDTO() {}

    public DelayedDeliveryDTO(
            Long deliveryId,
            String driverName,
            Long orderId,
            Double latitude,
            Double longitude,
            Double estimatedArrival,
            LocalDateTime updatedAt
    ) {
        this.deliveryId = deliveryId;
        this.driverName = driverName;
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedArrival = estimatedArrival;
        this.updatedAt = updatedAt;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public String getDriverName() {
        return driverName;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getEstimatedArrival() {
        return estimatedArrival;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
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
            return new DelayedDeliveryDTO(
                    deliveryId,
                    driverName,
                    orderId,
                    latitude,
                    longitude,
                    estimatedArrival,
                    updatedAt
            );
        }
    }
}