package com.team05.fooddelivery.order.messaging.publishers;

import java.math.BigDecimal;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.OrderPlacedEvent;
import com.team05.fooddelivery.order.model.Order;

@Component
public class OrderPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

        public void publishOrderCompletedEvent(Order order) {

            OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                BigDecimal.valueOf(order.getTotalAmount())
            );

            rabbitTemplate.convertAndSend("order.events", "order.completed", completedEvent);
        }

        public void publishOrderCancelledEvent(Order order, String reason) {
            OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                reason
            );
            rabbitTemplate.convertAndSend("order.events", "order.cancelled", cancelledEvent);
        }

        public void publishOrderPlacedEvent(Order order) {
            OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                BigDecimal.valueOf(order.getTotalAmount())
            );
            rabbitTemplate.convertAndSend("order.events", "order.placed", placedEvent);
        }
}
