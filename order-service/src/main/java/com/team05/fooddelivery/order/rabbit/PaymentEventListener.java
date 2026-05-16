package com.team05.fooddelivery.order.rabbit;

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
    private final OrderService orderService;

    
    public PaymentEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    
    @RabbitHandler
    public void handlePaymentInitiatedEvent(PaymentInitiatedEvent event) {
        // orderService.handlePaymentInitiatedEvent(event);
    }

    @RabbitHandler
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        // orderService.handlePaymentCompletedEvent(event);
    }

    @RabbitHandler
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        // orderService.handlePaymentFailedEvent(event);
    }

    @RabbitHandler
    public void handlePaymentRefundedEvent(PaymentRefundedEvent event) {
        // orderService.handlePaymentRefundedEvent(event);
    }


}
