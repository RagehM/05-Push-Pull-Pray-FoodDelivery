package com.team05.fooddelivery.order.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team05.fooddelivery.order.factory.EventFactory;

public class MongoEventLogger implements EntityObserver {

    private static final Logger logger = LoggerFactory.getLogger(MongoEventLogger.class);

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            // later we will connect this to Mongo + Factory
            Object event = EventFactory.createEvent(eventType, payload);
            logger.info("Event created: {}", event);
        } catch (Exception e) {
            // VERY IMPORTANT: must NOT crash app
            logger.warn("Failed to log event", e);
        }
    }
}