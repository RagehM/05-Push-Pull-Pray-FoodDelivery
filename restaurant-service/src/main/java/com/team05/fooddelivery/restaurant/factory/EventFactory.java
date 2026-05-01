package com.team05.fooddelivery.restaurant.factory;

import com.team05.fooddelivery.restaurant.model.mongo.RestaurantEvent;
import com.team05.shared.factory.EventFactoryBase;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.model.mongo.MongoEvent.EventType;

import java.util.Map;

// Section 3.7 — Factory Pattern
// Creates the right MongoDB event object based on the EventType
// For restaurant-service, only RESTAURANT type is supported
public class EventFactory implements EventFactoryBase {

    @Override
    public MongoEvent createEvent(EventType eventType, Map<String, Object> params) {

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) params.get("details");

        switch (eventType) {
            case RESTAURANT:
                return new RestaurantEvent(
                        (Long) params.get("restaurantId"),
                        (String) params.get("action"),
                        details);
            case AUTH:
                throw new UnsupportedOperationException("AUTH events not supported in Restaurant Service");
            case ORDER:
                throw new UnsupportedOperationException("ORDER events not supported in Restaurant Service");
            case DELIVERY:
                throw new UnsupportedOperationException("DELIVERY events not supported in Restaurant Service");
            case PAYMENT_AUDIT:
                throw new UnsupportedOperationException("PAYMENT_AUDIT events not supported in Restaurant Service");
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}