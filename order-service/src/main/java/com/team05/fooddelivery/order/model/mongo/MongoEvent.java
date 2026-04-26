package com.team05.fooddelivery.order.model.mongo;

import java.time.LocalDateTime;
import java.util.Map;

public interface MongoEvent {
    String getId();
    LocalDateTime getTimestamp();
    String getAction();
    Map<String, Object> getDetails();

    public static enum EventType {
        AUTH,
        RESTAURANT,
        ORDER,
        DELIVERY,
        PAYMENT_AUDIT
    }
}