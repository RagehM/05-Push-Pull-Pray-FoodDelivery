package com.team05.shared.observer;

import java.util.Map;

import com.team05.shared.factory.EventFactoryBase;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.repository.mongo.MongoEventRepository;
import com.team05.shared.observer.EntityObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoEventLogger<T extends MongoEvent> implements EntityObserver {

    private static final Logger logger = LoggerFactory.getLogger(MongoEventLogger.class);

    private final MongoEventRepository<T, String> loggerRepository;
    private final EventType loggerEventType;
    private final EventFactoryBase eventFactory;

    public MongoEventLogger(MongoEventRepository<T, String> loggerRepo, EventType eventType, EventFactoryBase eventFactory) {
        this.loggerRepository = loggerRepo;
        this.loggerEventType = eventType;
        this.eventFactory = eventFactory;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            ((Map<String, Object>) payload).put("action", eventType);
            @SuppressWarnings("unchecked") // Flags this typecast as unsafe with warning if suppressed is not used
            T event = (T) eventFactory.createEvent(this.loggerEventType, (Map<String, Object>) payload);
            
            // save to mongo
            this.loggerRepository.save(event);

            logger.info("Event created: {}", event);
        } catch (Exception e) {
            logger.warn("Failed to log event", e);
        }
    }
    
}