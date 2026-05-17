package com.team05.fooddelivery.order.saga;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.team05.fooddelivery.contracts.events.PaymentInitiatedEvent;
import com.team05.fooddelivery.contracts.events.PaymentCompletedEvent;
import com.team05.fooddelivery.contracts.events.PaymentFailedEvent;
import com.team05.fooddelivery.contracts.events.PaymentRefundedEvent;

@Component
@RabbitListener(queues = "order.saga-feedback")
public class PaymentEventListener {
    private final SagaTriggerService sagaTriggerService;

    
    public PaymentEventListener(SagaTriggerService sagaTriggerService) {
        this.sagaTriggerService = sagaTriggerService;
    }

    
    @RabbitHandler
    public void handlePaymentInitiatedEvent(
        PaymentInitiatedEvent event,
        @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        try{
            setupMdc("payment.initiated", correlationId, event.orderId(), null, null, null, event.paymentId());
            sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.initiated");
            System.out.println("Payment Initiated Event Received");
        } finally {
            clearMdc();
        }

    }

    @RabbitHandler
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        try{
            setupMdc("payment.completed", null, event.orderId(), null, null, null, event.paymentId());
            sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.completed");
            System.out.println("Payment Completed Event Received");
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        try{
            setupMdc("payment.failed", null, event.orderId(), null, null, null, event.paymentId());
            sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.failed");
            System.out.println("Payment Failed Event Received");
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler
    public void handlePaymentRefundedEvent(PaymentRefundedEvent event) {
        try{
            setupMdc("payment.refunded", null, event.orderId(), null, null, null, event.paymentId());
            sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.refunded");
            System.out.println("Payment Refunded Event Received");
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
