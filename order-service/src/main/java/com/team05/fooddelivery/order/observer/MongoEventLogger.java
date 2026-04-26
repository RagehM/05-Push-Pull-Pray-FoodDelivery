package com.team05.fooddelivery.order.observer;

import com.team05.fooddelivery.order.factory.EventFactory;
import com.team05.fooddelivery.order.model.mongo.MongoEvent;
import com.team05.fooddelivery.order.model.mongo.MongoEvent.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MongoEventLogger implements EntityObserver {

    private static final Logger logger = LoggerFactory.getLogger(MongoEventLogger.class);

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            if (!(payload instanceof Map)) {
                throw new IllegalArgumentException("Payload must be Map<String, Object>");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) payload;

            MongoEvent event = EventFactory.createEvent(EventType.ORDER, params);

            logger.info("Event created: {}", event);

        } catch (Exception e) {
            logger.warn("Failed to log event", e);
        }
    }
}