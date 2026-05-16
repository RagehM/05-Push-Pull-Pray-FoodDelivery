package com.team05.fooddelivery.order.saga;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.team05.fooddelivery.contracts.events.PaymentInitiatedEvent;
import com.team05.fooddelivery.contracts.events.PaymentCompletedEvent;
import com.team05.fooddelivery.contracts.events.PaymentFailedEvent;
import com.team05.fooddelivery.contracts.events.PaymentRefundedEvent;
import com.team05.fooddelivery.order.service.OrderService;

@Component
@RabbitListener(queues = "order.saga-feedback")
public class PaymentEventListener {
    private final SagaTriggerService sagaTriggerService;

    
    public PaymentEventListener(SagaTriggerService sagaTriggerService) {
        this.sagaTriggerService = sagaTriggerService;
    }

    
    @RabbitHandler
    public void handlePaymentInitiatedEvent(PaymentInitiatedEvent event) {
        sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.initiated");
        System.out.println("Payment Initiated Event Received");
    }

    @RabbitHandler
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.completed");
        System.out.println("Payment Completed Event Received");
    }

    @RabbitHandler
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.failed");
        System.out.println("Payment Failed Event Received");
    }

    @RabbitHandler
    public void handlePaymentRefundedEvent(PaymentRefundedEvent event) {
        sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "payment.refunded");
        System.out.println("Payment Refunded Event Received");
    }


}
