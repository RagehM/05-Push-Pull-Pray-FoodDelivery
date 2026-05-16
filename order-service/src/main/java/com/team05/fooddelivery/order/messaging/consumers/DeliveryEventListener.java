package com.team05.fooddelivery.order.messaging.consumers;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.team05.fooddelivery.contracts.events.DeliveryCreatedEvent;
import com.team05.fooddelivery.order.service.OrderService;

@Component
@RabbitListener(queues = "order.saga-feedback")
public class DeliveryEventListener {
    private final OrderService orderService;

    public DeliveryEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitHandler
    public void handleDeliveryCreatedEvent(DeliveryCreatedEvent event) {
        orderService.processDeliveryCreatedAndPaymentInitiatedEvent(event.orderId(), "delivery.created");
    }
}
