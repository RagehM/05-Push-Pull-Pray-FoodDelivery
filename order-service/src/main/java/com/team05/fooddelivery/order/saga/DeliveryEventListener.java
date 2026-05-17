package com.team05.fooddelivery.order.saga;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.team05.fooddelivery.contracts.events.DeliveryCreatedEvent;

@Component
@RabbitListener(queues = "order.saga-feedback")
public class DeliveryEventListener {
    private final SagaTriggerService sagaTriggerService;

    public DeliveryEventListener(SagaTriggerService sagaTriggerService) {
        this.sagaTriggerService = sagaTriggerService;
    }

    @RabbitHandler
    public void handleDeliveryCreatedEvent(
        DeliveryCreatedEvent event,
        @Header(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        try{
            setupMdc("delivery.created", correlationId, event.orderId(), null, null, event.deliveryId(), null);
            sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "delivery.created");
            System.out.println("Delivery Created Event Received");
        } finally {
            clearMdc();
        }
    }

    private void setupMdc(String routingKey, String correlationId, Long orderId, Long userId, Long restaurantId, Long deliveryId, Long paymentId) {
        if (routingKey != null)    MDC.put("routingKey",    routingKey);
        if (correlationId != null) MDC.put("correlationId", correlationId);
        if (orderId != null)       MDC.put("orderId",       orderId.toString());
        if (userId != null)        MDC.put("userId",        userId.toString());
        if (restaurantId != null)  MDC.put("restaurantId",  restaurantId.toString());
        if (deliveryId != null)    MDC.put("deliveryId",    deliveryId.toString());
        if (paymentId != null)     MDC.put("paymentId",     paymentId.toString());
    }

    private void clearMdc() {
        MDC.remove("routingKey");
        MDC.remove("correlationId");
        MDC.remove("orderId");
        MDC.remove("userId");
        MDC.remove("restaurantId");
        MDC.remove("deliveryId");
        MDC.remove("paymentId");
    }
}
