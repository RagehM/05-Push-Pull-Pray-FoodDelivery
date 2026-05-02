package com.team05.fooddelivery.order.model.mongo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.team05.shared.model.mongo.MongoEvent;

import com.mongodb.lang.NonNull;

@Document("order_events")
public class OrderEvent implements MongoEvent{
    @Id
    private String id;
    @Indexed
    @NonNull
    private Long orderId;
    @NonNull
    private String action;
    @NonNull
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<String, Object>();

    public OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public OrderEvent(Long orderId, String action, Map<String, Object> details) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        this.orderId = orderId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    public String getId() { return id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public static final class OrderEventActions {
        public static final String ANALYTICS_VIEWED = "ANALYTICS_VIEWED";
        public static final String INTERACTION_RECORDED = "INTERACTION_RECORDED";
        public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
        public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
        public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
        public static final String ITEMS_ADDED = "ITEMS_ADDED";
        public static final String ORDER_CREATED = "ORDER_CREATED";
        public static final String ORDER_DELETED = "ORDER_DELETED";

    }
}
