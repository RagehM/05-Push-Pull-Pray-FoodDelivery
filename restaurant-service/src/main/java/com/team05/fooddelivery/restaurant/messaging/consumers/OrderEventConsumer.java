package com.team05.fooddelivery.restaurant.messaging.consumers;

import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.OrderPlacedEvent;
import com.team05.fooddelivery.restaurant.config.RabbitMQ;
import com.team05.fooddelivery.restaurant.service.RestaurantSagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(
        queues = RabbitMQ.RESTAURANT_ORDER_SAGA_QUEUE,
        containerFactory = "rabbitListenerContainerFactory"
)
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final RestaurantSagaService restaurantSagaService;

    public OrderEventConsumer(RestaurantSagaService restaurantSagaService) {
        this.restaurantSagaService = restaurantSagaService;
    }

    @RabbitHandler
    public void onOrderPlaced(
            OrderPlacedEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        setupMdc(RabbitMQ.ORDER_PLACED_ROUTING_KEY, correlationId, event.restaurantId(), event.orderId());
        try {
            log.info("Consuming order.placed for orderId={}", event.orderId());
            restaurantSagaService.handleOrderPlaced(event);
            log.info("Processed order.placed for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("Failed to process order.placed for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler
    public void onOrderCompleted(
            OrderCompletedEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        setupMdc(RabbitMQ.ORDER_COMPLETED_ROUTING_KEY, correlationId, event.restaurantId(), event.orderId());
        try {
            log.info("Consuming order.completed for orderId={}", event.orderId());
            restaurantSagaService.handleOrderCompleted(event);
            log.info("Processed order.completed for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("Failed to process order.completed for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler
    public void onOrderCancelled(
            OrderCancelledEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {
        setupMdc(RabbitMQ.ORDER_CANCELLED_ROUTING_KEY, correlationId, event.restaurantId(), event.orderId());
        try {
            log.info("Consuming order.cancelled for orderId={}", event.orderId());
            restaurantSagaService.handleOrderCancelled(event);
            log.info("Processed order.cancelled for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("Failed to process order.cancelled for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object unknownPayload) {
        log.warn("restaurant.order.saga-listener received unknown message type: {}",
                unknownPayload == null ? "null" : unknownPayload.getClass().getName());
    }

    private void setupMdc(String routingKey, String correlationId, Long restaurantId, Long orderId) {
        if (routingKey != null) {
            MDC.put("routingKey", routingKey);
        }
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        if (restaurantId != null) {
            MDC.put("restaurantId", restaurantId.toString());
        }
        if (orderId != null) {
            MDC.put("orderId", orderId.toString());
        }
    }

    private void clearMdc() {
        MDC.remove("routingKey");
        MDC.remove("correlationId");
        MDC.remove("restaurantId");
        MDC.remove("orderId");
    }
}
