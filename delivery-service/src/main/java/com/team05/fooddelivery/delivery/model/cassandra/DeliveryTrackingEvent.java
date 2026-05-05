package com.team05.fooddelivery.delivery.model.cassandra;

import java.time.Instant;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Cassandra time-series entity for delivery tracking events.
 * Table: delivery_tracking_events

 * Compound Key: DeliveryTrackingEventKey
 * - Partition key: delivery_id (groups all tracking events for a single delivery)
 * - Clustering key: timestamp DESC (orders events newest-first)

 * Queries MUST include delivery_id in the WHERE clause (partition key requirement).
 */
@Table("delivery_tracking_events")
public class DeliveryTrackingEvent {

    @PrimaryKey
    private DeliveryTrackingEventKey key;

    @Column("status")
    private String status;

    @Column("driver_name")
    private String driverName;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    @Column("notes")
    private String notes;

    // Constructors
    public DeliveryTrackingEvent() {
    }

    public DeliveryTrackingEvent(Long deliveryId, Instant timestamp, String status, String driverName,
                                 Double latitude, Double longitude, String notes) {
        this.key = new DeliveryTrackingEventKey(deliveryId, timestamp);
        this.status = status;
        this.driverName = driverName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    // Getters and Setters
    public DeliveryTrackingEventKey getKey() {
        return key;
    }

    public void setKey(DeliveryTrackingEventKey key) {
        this.key = key;
    }

    public Long getDeliveryId() {
        return key != null ? key.getDeliveryId() : null;
    }

    public void setDeliveryId(Long deliveryId) {
        if (key == null) {
            key = new DeliveryTrackingEventKey();
        }
        key.setDeliveryId(deliveryId);
    }

    public Instant getTimestamp() {
        return key != null ? key.getTimestamp() : null;
    }

    public void setTimestamp(Instant timestamp) {
        if (key == null) {
            key = new DeliveryTrackingEventKey();
        }
        key.setTimestamp(timestamp);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}







