package com.team05.fooddelivery.delivery.factory;

import com.team05.shared.factory.EventFactoryBase;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.fooddelivery.delivery.model.mongo.DeliveryEvent;

import java.util.Map;

public class DeliveryEventFactory implements EventFactoryBase {

    @Override
    public MongoEvent createEvent(MongoEvent.EventType eventType, Map<String, Object> params) {
        @SuppressWarnings("unchecked") // Flags this typecast as unsafe with warning if suppressed is not used
        Map<String, Object> details = (Map<String, Object>)params.get("details");
        // details is optional (nullable) and used by all events

        switch (eventType) {
            case AUTH:
                throw new UnsupportedOperationException("AUTH events are not supported in Checkout Service");
            case RESTAURANT:
                throw new UnsupportedOperationException("RESTAURANT events are not supported in Checkout Service");
            case ORDER:
                throw  new UnsupportedOperationException("ORDER events are not supported in Checkout Service");
            case DELIVERY:
                // [S4-F2, S4-F4, S4-F7, CRUD] Handle delivery-related events
                Long deliveryId = (Long) params.get("deliveryId");
                String action = (String) params.get("action");
                // timestamp is automatically set to now() in DeliveryEvent constructor
                return new DeliveryEvent(deliveryId, action, details);
            case PAYMENT_AUDIT:
                throw new UnsupportedOperationException("PAYMENT_AUDIT events are not supported in Delivery Service");
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}

