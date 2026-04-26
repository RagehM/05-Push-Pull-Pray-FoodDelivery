package com.team05.fooddelivery.order.factory;

import java.util.Map;

import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.model.mongo.OrderEvent;

import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.factory.EventFactoryBase;

public class EventFactory implements EventFactoryBase {

    @Override
    public MongoEvent createEvent(EventType eventType, Map<String, Object> params) {
        @SuppressWarnings("unchecked") // Flags this typecast as unsafe with warning if suppressed is not used
        Map<String, Object> details = (Map<String, Object>)params.get("details");
        // details is optional (nullable) and used by all events
        
        switch (eventType) {
            case AUTH:
                throw new UnsupportedOperationException("AUTH events are not supported in Order Service");
            case RESTAURANT:
                throw new UnsupportedOperationException("RESTAURANT events are not supported in Order Service");
            case ORDER:
                OrderEvent orderEvent = new OrderEvent();
                orderEvent.setOrderId(((Long)params.get("orderId")));
                orderEvent.setAction((String)params.get("action"));
                // orderEvent.setTimestamp(LocalDateTime.now()); no need, timestamp is set in constructor
                orderEvent.setDetails(details);
                return orderEvent;
            case DELIVERY:
                throw new UnsupportedOperationException("DELIVERY events are not supported in Order Service");
            case PAYMENT_AUDIT:
                throw new UnsupportedOperationException("PAYMENT_AUDIT events are not supported in Order Service");
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}