package com.team05.fooddelivery.delivery.model.cassandra;

import java.time.Instant;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Cassandra time-series entity for delivery tracking events.
 * Table: delivery_tracking_events
 *
 * Partition key: delivery_id (groups all tracking events for a single delivery)
 * Clustering key: timestamp DESC (orders events newest-first)
 */
@Table("delivery_tracking_events")
public class DeliveryTrackingEvent {

    @PrimaryKeyColumn(name = "delivery_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long deliveryId;

    @PrimaryKeyColumn(name = "timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant timestamp;

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

    public DeliveryTrackingEvent() {
    }

    public DeliveryTrackingEvent(Long deliveryId, Instant timestamp, String status, String driverName,
                                 Double latitude, Double longitude, String notes) {
        this.deliveryId = deliveryId;
        this.timestamp = timestamp;
        this.status = status;
        this.driverName = driverName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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
