package com.team05.fooddelivery.order.factory;

import java.util.HashMap;
import java.util.Map;

public class EventFactory {

    public static Map<String, Object> createEvent(String eventType, Object payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", eventType);
        event.put("data", payload);
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }
}