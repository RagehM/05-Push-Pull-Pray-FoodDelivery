package com.team05.fooddelivery.delivery.model.mongo;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Common interface for all MongoDB event documents.
 * Enables polymorphic handling via EventFactory.
 */
public interface MongoEvent {

    /**
     * @return MongoDB ObjectId as String
     */
    String getId();

    /**
     * @return Timestamp when the event occurred
     */
    LocalDateTime getTimestamp();

    /**
     * @return The action identifier (e.g., REGISTERED, TRACKING_RECORDED)
     */
    String getAction();

    /**
     * @return Additional event context and metadata
     */
    Map<String, Object> getDetails();
}

