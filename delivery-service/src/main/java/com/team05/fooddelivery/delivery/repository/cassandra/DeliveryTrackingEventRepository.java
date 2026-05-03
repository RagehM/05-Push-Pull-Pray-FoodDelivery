package com.team05.fooddelivery.delivery.repository.cassandra;

import java.time.Instant;
import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.delivery.model.cassandra.DeliveryTrackingEvent;
import com.team05.fooddelivery.delivery.model.cassandra.DeliveryTrackingEventKey;

/**
 * Cassandra repository for DeliveryTrackingEvent time-series data.
 * Table: delivery_tracking_events

 * NOTE: All queries must include delivery_id (partition key) in the WHERE clause.
 */
@Repository
public interface DeliveryTrackingEventRepository extends CassandraRepository<DeliveryTrackingEvent, DeliveryTrackingEventKey> {

    /**
     * Find all tracking events for a delivery, ordered by timestamp (newest first).
     * Cassandra partition key required: delivery_id.
     */
    List<DeliveryTrackingEvent> findByKeyDeliveryId(Long deliveryId);

    /**
     * Find all tracking events for a delivery within a timestamp range.
     * Returns events ordered by timestamp descending (newest first).
     */
    @Query("SELECT * FROM delivery_tracking_events WHERE delivery_id = ?0 AND timestamp >= ?1 AND timestamp <= ?2 ALLOW FILTERING")
    List<DeliveryTrackingEvent> findByKeyDeliveryIdAndKeyTimestampBetween(Long deliveryId, Instant startTime, Instant endTime);

    /**
     * Find all tracking events for a delivery with a specific status.
     * Uses ALLOW FILTERING for secondary column query.
     */
    @Query("SELECT * FROM delivery_tracking_events WHERE delivery_id = ?0 AND status = ?1 ALLOW FILTERING")
    List<DeliveryTrackingEvent> findBykeyDeliveryIdAndStatus(Long deliveryId, String status);

    long countByKeyDeliveryId(Long deliveryId);

    // Returns rows for the partition ordered by event_time DESC.
// This matches the table's required CLUSTERING ORDER BY (event_time DESC).
    List<DeliveryTrackingEvent> findByKeyDeliveryIdOrderByKeyTimestampDesc(Long deliveryId);
}





