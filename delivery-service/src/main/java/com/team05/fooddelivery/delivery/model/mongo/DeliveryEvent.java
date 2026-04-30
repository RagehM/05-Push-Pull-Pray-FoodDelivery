package com.team05.fooddelivery.delivery.model.mongo;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.team05.shared.model.mongo.MongoEvent;

/**
 * MongoDB document for delivery service event logging.
 * Collection: delivery_events

 * Logs delivery-related actions such as tracking recordings and analytics views.
 * Implements MongoEvent interface for polymorphic factory handling.
 */
@Document(collection = "delivery_events")
public class DeliveryEvent implements MongoEvent {

    @Id
    private String id;

    @Indexed
    private Long deliveryId;

    private String action;

    private LocalDateTime timestamp;

    private Map<String, Object> details;

    // Constructors
    public DeliveryEvent() {
    }

    public DeliveryEvent(Long deliveryId, String action, LocalDateTime timestamp, Map<String, Object> details) {
        if (deliveryId == null){
            throw new IllegalArgumentException("deliveryId must not be null");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }

        this.deliveryId = deliveryId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    // MongoEvent interface methods
    @Override
    public String getId() {
        return id;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    // Getters and Setters
    public void setId(String id) {
        this.id = id;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}

