package com.team05.fooddelivery.restaurant.messaging.publishers;

import com.team05.fooddelivery.contracts.events.RestaurantRatedEvent;
import com.team05.fooddelivery.contracts.events.RestaurantStatusChangedEvent;
import com.team05.fooddelivery.restaurant.config.RabbitMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RestaurantEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestaurantEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RestaurantEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStatusChanged(RestaurantStatusChangedEvent event) {
        publish(RabbitMQ.RESTAURANT_STATUS_CHANGED_ROUTING_KEY, event, event.restaurantId());
    }

    public void publishRated(RestaurantRatedEvent event) {
        publish(RabbitMQ.RESTAURANT_RATED_ROUTING_KEY, event, event.restaurantId());
    }

    private void publish(String routingKey, Object payload, Long restaurantId) {
        try {
            if (restaurantId != null) {
                MDC.put("restaurantId", restaurantId.toString());
            }
            MDC.put("routingKey", routingKey);
            rabbitTemplate.convertAndSend(
                    RabbitMQ.RESTAURANT_EVENTS_EXCHANGE,
                    routingKey,
                    payload,
                    message -> {
                        String correlationId = MDC.get("correlationId");
                        if (correlationId != null && !correlationId.isBlank()) {
                            message.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
                        }
                        return message;
                    }
            );
            log.info("Published {} for restaurantId={}", routingKey, restaurantId);
        } finally {
            MDC.remove("routingKey");
            if (restaurantId != null) {
                MDC.remove("restaurantId");
            }
        }
    }
}
