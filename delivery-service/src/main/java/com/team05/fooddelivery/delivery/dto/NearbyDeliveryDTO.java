package com.team05.fooddelivery.delivery.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NearbyDeliveryDTO {

    private final Long deliveryId;
    private final String driverName;
    private final Long orderId;
    private final Double latitude;
    private final Double longitude;
    private final Double distanceKm;

    private NearbyDeliveryDTO(Builder builder) {
        this.deliveryId = builder.deliveryId;
        this.driverName = builder.driverName;
        this.orderId = builder.orderId;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.distanceKm = builder.distanceKm;
    }

    public Long getDeliveryId() { return deliveryId; }
    public String getDriverName() { return driverName; }
    public Long getOrderId() { return orderId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getDistanceKm() { return distanceKm; }

    @JsonCreator
    private NearbyDeliveryDTO(
            @JsonProperty("deliveryId") Long deliveryId,
            @JsonProperty("driverName") String driverName,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("distanceKm") Double distanceKm) {
        this.deliveryId = deliveryId;
        this.driverName = driverName;
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
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
        private Double distanceKm;

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

        public Builder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public NearbyDeliveryDTO build() {
            return new NearbyDeliveryDTO(this);
        }
    }
}