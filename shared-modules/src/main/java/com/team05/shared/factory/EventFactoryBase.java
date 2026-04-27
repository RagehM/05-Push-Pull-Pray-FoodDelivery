package com.team05.shared.factory;

import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.model.mongo.MongoEvent;
import java.util.Map;

public interface EventFactoryBase {
    MongoEvent createEvent(EventType eventType, Map<String, Object> params);
}