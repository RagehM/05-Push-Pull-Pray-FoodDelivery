package com.team05.fooddelivery.delivery.model.cassandra;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

/**
 * Compound primary key for DeliveryTrackingEvent in Cassandra.

 * PARTITION KEY:
 * - delivery_id: Groups all tracking events for a single delivery on the same node.

 * CLUSTERING KEY:
 * - timestamp: Orders events within each partition (newest first).
 *   Each tracking event is uniquely identified by (delivery_id, timestamp).

 * Physical Storage:
 * - Cassandra hashes delivery_id to determine which node stores this row.
 * - Rows are sorted on disk by timestamp in DESCENDING order (newest events come first).
 * - No additional ORDER BY needed when querying.
 */
@PrimaryKeyClass
public class DeliveryTrackingEventKey implements Serializable {

    /**
     * PARTITION KEY (ordinal = 0, type = PARTITIONED)
     * Determines which Cassandra node stores this row.
     * All rows with the same delivery_id live together in the same partition.
     */
    @PrimaryKeyColumn(name = "delivery_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long deliveryId;

    /**
     * CLUSTERING KEY (ordinal = 1, type = CLUSTERED)
     * Determines sort order WITHIN the partition.
     * ordering = DESCENDING: Newest event_time sorts first, so queries return recent events first.
     */
    @PrimaryKeyColumn(name = "event_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant eventTime;

    // Constructors
    public DeliveryTrackingEventKey() {
    }

    public DeliveryTrackingEventKey(Long deliveryId, Instant eventTime) {
        this.deliveryId = deliveryId;
        this.eventTime = eventTime;
    }

    // Getters and Setters
    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {
        return "DeliveryTrackingEventKey{" +
                "deliveryId=" + deliveryId +
                ", eventTime=" + eventTime +
                '}';
    }
}


