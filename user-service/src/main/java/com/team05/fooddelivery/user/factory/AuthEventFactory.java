package com.team05.fooddelivery.user.factory;

import com.team05.fooddelivery.user.model.mongo.AuthEvent;
import com.team05.shared.factory.EventFactoryBase;
import com.team05.shared.model.mongo.MongoEvent;

import java.util.Map;

public class AuthEventFactory implements EventFactoryBase {

    @Override
    public MongoEvent createEvent(MongoEvent.EventType eventType, Map<String, Object> params) {
        @SuppressWarnings("unchecked") // Flags this typecast as unsafe with warning if suppressed is not used
        Map<String, Object> details = (Map<String, Object>)params.get("details");
        // details is optional (nullable) and used by all events

        switch (eventType) {
            case AUTH:
                Long userId = (Long)params.get("userId");
                String action = (String)params.get("action");
                String method = (String)params.get("method");
                Double amount = (Double)params.get("amount");
                return new AuthEvent(userId, action, details);
            case RESTAURANT:
                throw new UnsupportedOperationException("RESTAURANT events are not supported in Checkout Service");
            case ORDER:
                throw  new UnsupportedOperationException("ORDER events are not supported in Checkout Service");
            case DELIVERY:
                throw new UnsupportedOperationException("DELIVERY events are not supported in Checkout Service");
            case PAYMENT_AUDIT:
                throw new UnsupportedOperationException("PAYMENT_AUDIT events are not supported in User Service");
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}