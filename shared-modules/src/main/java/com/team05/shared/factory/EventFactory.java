package com.team05.shared.factory;

import com.team05.shared.model.mongo.MongoEvent.EventType;
import com.team05.shared.model.mongo.MongoEvent;
import com.team05.shared.model.mongo.OrderEvent;
import com.team05.shared.model.mongo.PaymentAuditEvent;
import com.team05.shared.model.mongo.DeliveryEvent;
import com.team05.shared.model.mongo.RestaurantEvent;
import com.team05.shared.model.mongo.AuthEvent;
import java.util.Map;

public class EventFactory {
    

    public static MongoEvent createEvent(EventType eventType, Map<String, Object> params){
        @SuppressWarnings("unchecked") // Flags this typecast as unsafe with warning if suppressed is not used
        Map<String, Object> details = (Map<String, Object>)params.get("details");
        // details is optional (nullable) and used by all events
        
        switch (eventType) {
            case AUTH:
                Long userId = (Long)params.get("userId");
                String action = (String)params.get("action");
                return new AuthEvent(userId, action, details);
            case RESTAURANT:
                return new RestaurantEvent(
                        (Long) params.get("restaurantId"),
                        (String) params.get("action"),
                        details);
            case ORDER:
                OrderEvent orderEvent = new OrderEvent(
                    (Long)params.get("orderId"),
                    (String)params.get("action"),
                    details
                );
                return orderEvent;
            case DELIVERY:
                // [S4-F2, S4-F4, S4-F7, CRUD] Handle delivery-related events
                Long deliveryId = (Long) params.get("deliveryId");
                String deliveryAction = (String) params.get("action");
                // timestamp is automatically set to now() in DeliveryEvent constructor
                return new DeliveryEvent(deliveryId, deliveryAction, details);
            case PAYMENT_AUDIT:
                Long paymentId = (Long)params.get("paymentId");
                String auditAction = (String)params.get("action");
                String method = (String)params.get("method");
                Double amount = (Double)params.get("amount");
                return new PaymentAuditEvent(paymentId, auditAction, method, amount, details);
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }
}