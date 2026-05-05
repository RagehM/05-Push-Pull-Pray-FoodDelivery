package com.team05.fooddelivery.delivery.repository.cassandra;

import java.time.Instant;
import java.util.List;

import org.springframework.data.cassandra.core.mapping.MapId;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.delivery.model.cassandra.DeliveryTrackingEvent;

/**
 * Cassandra repository for DeliveryTrackingEvent time-series data.
 * Table: delivery_tracking_events
 *
 * All queries must include delivery_id (partition key) in the WHERE clause.
 */
@Repository
public interface DeliveryTrackingEventRepository extends CassandraRepository<DeliveryTrackingEvent, MapId> {

    List<DeliveryTrackingEvent> findByDeliveryIdOrderByEventTimeDesc(Long deliveryId);

    @Query("SELECT * FROM delivery_tracking_events WHERE delivery_id = ?0 AND event_time >= ?1 AND event_time <= ?2 ORDER BY event_time DESC")
    List<DeliveryTrackingEvent> findByDeliveryIdInTimeRange(Long deliveryId, Instant startTime, Instant endTime);

    @Query("SELECT * FROM delivery_tracking_events WHERE delivery_id = ?0 AND status = ?1 ALLOW FILTERING")
    List<DeliveryTrackingEvent> findByDeliveryIdAndStatus(Long deliveryId, String status);

    long countByDeliveryId(Long deliveryId);
}
