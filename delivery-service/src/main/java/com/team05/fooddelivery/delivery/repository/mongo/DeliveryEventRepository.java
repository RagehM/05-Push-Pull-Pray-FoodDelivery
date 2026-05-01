package com.team05.fooddelivery.delivery.repository.mongo;

import java.time.LocalDateTime;
import java.util.List;

import com.team05.shared.repository.mongo.MongoEventRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.delivery.model.mongo.DeliveryEvent;

/**
 * MongoDB repository for DeliveryEvent documents.
 * Collection: delivery_events
 */
@Repository
public interface DeliveryEventRepository extends MongoEventRepository<DeliveryEvent, String> {

    /**
     * Find all events for a specific delivery.
     */
    List<DeliveryEvent> findByDeliveryId(Long deliveryId);

    /**
     * Find all events for a delivery with a specific action.
     */
    List<DeliveryEvent> findByDeliveryIdAndAction(Long deliveryId, String action);

    /**
     * Find all events within a time range.
     */
    List<DeliveryEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find all events for a delivery within a time range.
     */
    List<DeliveryEvent> findByDeliveryIdAndTimestampBetween(Long deliveryId, LocalDateTime start, LocalDateTime end);

    /**
     * Find the most recent event for a delivery.
     */
    @Query(value = "{ 'deliveryId': ?0 }", sort = "{ 'timestamp': -1 }", fields = "{}")
    DeliveryEvent findMostRecentByDeliveryId(Long deliveryId);
}

