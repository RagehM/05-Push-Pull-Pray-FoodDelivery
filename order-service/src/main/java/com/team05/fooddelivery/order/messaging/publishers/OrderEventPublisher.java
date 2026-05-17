package com.team05.fooddelivery.order.messaging.publishers;

import java.math.BigDecimal;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.OrderPlacedEvent;
import com.team05.fooddelivery.order.model.Order;

@Component
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

        public void publishOrderCompletedEvent(Order order) {

            OrderCompletedEvent completedEvent = new OrderCompletedEvent(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                BigDecimal.valueOf(order.getTotalAmount())
            );
            publish("order.completed", completedEvent, order.getId());
            // rabbitTemplate.convertAndSend("order.events", "order.completed", completedEvent);
        }

        public void publishOrderCancelledEvent(Order order, String reason) {
            OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                reason
            );
            publish("order.cancelled", cancelledEvent, order.getId());
        }

        public void publishOrderPlacedEvent(Order order) {
            OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                BigDecimal.valueOf(order.getTotalAmount())
            );
            publish("order.placed", placedEvent, order.getId());
        }

        private void publish(String routingKey, Object payload, Long orderId) {
            try {
                if (orderId != null) {
                    MDC.put("orderId", orderId.toString());
                }
                MDC.put("routingKey", routingKey);
                String correlationId = MDC.get("correlationId");
                rabbitTemplate.convertAndSend(
                        "order.events",
                        routingKey,
                        payload,
                        message -> {
                            message.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
                            return message;
                        }
                );
            } finally {
                MDC.remove("routingKey");
                if (orderId != null) {
                    MDC.remove("orderId");
                }
            }
        }
}
