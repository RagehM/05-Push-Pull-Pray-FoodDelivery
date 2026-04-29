package com.team05.fooddelivery.restaurant.model.mongo;

import com.mongodb.lang.NonNull;
import com.team05.shared.model.mongo.MongoEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Section 7.1.3 — RestaurantEvent MongoDB document
// Saved to the "restaurant_events" collection every time a write happens in restaurant-service
@Document("restaurant_events")
public class RestaurantEvent implements MongoEvent {

    @Id
    private String id;

    @Indexed
    @NonNull
    private Long restaurantId;

    @NonNull
    private String action;

    @NonNull
    private LocalDateTime timestamp;

    private Map<String, Object> details = new HashMap<>();

    public RestaurantEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public RestaurantEvent(Long restaurantId, String action, Map<String, Object> details) {
        this.restaurantId = restaurantId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    @Override
    public String getId() { return id; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getAction() { return action; }

    @Override
    public Map<String, Object> getDetails() { return details; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public void setAction(String action) { this.action = action; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    // Action constants from Section 7.1.3
    public static final class RestaurantEventActions {
        public static final String INDEXED = "INDEXED";
        public static final String UPDATED = "UPDATED";
        public static final String DASHBOARD_VIEWED = "DASHBOARD_VIEWED";
        public static final String DETAILS_UPDATED = "DETAILS_UPDATED";
        public static final String STATUS_CHANGED = "STATUS_CHANGED";
        public static final String REVIEW_ADDED = "REVIEW_ADDED";
        public static final String MENU_ITEM_TOGGLED = "MENU_ITEM_TOGGLED";
        public static final String RESTAURANT_CREATED = "RESTAURANT_CREATED";
        public static final String RESTAURANT_DELETED = "RESTAURANT_DELETED";
    }
}