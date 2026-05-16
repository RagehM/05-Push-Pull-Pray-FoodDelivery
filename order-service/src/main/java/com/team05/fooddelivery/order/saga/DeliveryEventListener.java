package com.team05.fooddelivery.order.saga;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
    public void handleDeliveryCreatedEvent(DeliveryCreatedEvent event) {
        sagaTriggerService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "delivery.created");
        System.out.println("Delivery Created Event Received");
    }
}
