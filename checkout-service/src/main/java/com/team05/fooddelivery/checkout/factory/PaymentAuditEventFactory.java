package com.team05.fooddelivery.checkout.factory;

import com.team05.fooddelivery.checkout.model.mongo.PaymentAuditEvent;
import com.team05.shared.factory.EventFactoryBase;
import com.team05.shared.model.mongo.MongoEvent;

import java.util.Map;

public class PaymentAuditEventFactory implements EventFactoryBase {

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
                throw new UnsupportedOperationException("DELIVERY events are not supported in Checkout Service");
            case PAYMENT_AUDIT:
                Long paymentId = (Long)params.get("paymentId");
                String action = (String)params.get("action");
                if ("ANALYTICS_VIEWED".equals(action)) {
                    PaymentAuditEvent event = new PaymentAuditEvent();
                    event.setAction(action);
                    event.setDetails(details);
                    return event;
                }
                String method = (String)params.get("method");
                Double amount = (Double)params.get("amount");
                return new PaymentAuditEvent(paymentId, action, method, amount, details);
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}
