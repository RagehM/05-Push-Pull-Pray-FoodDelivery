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

    // Explicit DESC ordering on both paths — sort order is independent of native table clustering schema
    List<DeliveryTrackingEvent> findByKeyDeliveryIdOrderByKeyTimestampDesc(Long deliveryId);

    // Explicit CQL avoids Spring Data's Between bound-reversal bug on DESC clustering keys
    @Query("SELECT * FROM delivery_tracking_events WHERE delivery_id = ?0 AND timestamp >= ?1 AND timestamp <= ?2 ORDER BY timestamp DESC")
    List<DeliveryTrackingEvent> findByKeyDeliveryIdInTimeRange(Long deliveryId, Instant startTime, Instant endTime);

    // status is a non-key column → requires ALLOW FILTERING
    @Query("SELECT * FROM delivery_tracking_events WHERE delivery_id = ?0 AND status = ?1 ALLOW FILTERING")
    List<DeliveryTrackingEvent> findByKeyDeliveryIdAndStatus(Long deliveryId, String status);

    long countByKeyDeliveryId(Long deliveryId);
}





