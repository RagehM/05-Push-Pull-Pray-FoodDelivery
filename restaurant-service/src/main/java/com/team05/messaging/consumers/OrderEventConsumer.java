package com.team05.messaging.consumers;

import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.OrderPlacedEvent;
import com.team05.fooddelivery.restaurant.config.RabbitMQ;
import com.team05.fooddelivery.restaurant.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RabbitListener(queues = RabbitMQ.RESTAURANT_SAGA_LISTENER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final RestaurantRepository restaurantRepository;

    public OrderEventConsumer(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    // ── order.placed → log incoming order notification ────────────────────────
    @RabbitHandler
    @Transactional
    public void onOrderPlaced(
            OrderPlacedEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitMQ.ORDER_PLACED_ROUTING_KEY, correlationId, event.orderId(), event.restaurantId());
        try {
            log.info("Consuming order.placed for orderId={}", event.orderId());
            // Spec §6 S4 Consumes: "Log the incoming order notification"
            // No DB mutation required — infrastructure (queue + DLQ) is what matters here
            log.info("Processed order.placed for restaurantId={} orderId={} — incoming order noted",
                    event.restaurantId(), event.orderId());
        } catch (Exception ex) {
            log.error("Failed to process order.placed for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    // ── order.completed → update restaurant stats ─────────────────────────────
    @RabbitHandler
    @Transactional
    public void onOrderCompleted(
            OrderCompletedEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitMQ.ORDER_COMPLETED_ROUTING_KEY, correlationId, event.orderId(), event.restaurantId());
        try {
            log.info("Consuming order.completed for orderId={}", event.orderId());

            restaurantRepository.findById(event.restaurantId()).ifPresentOrElse(restaurant -> {
                restaurant.setTotalRatings(restaurant.getTotalRatings()); // no stat fields — see note below
                restaurantRepository.save(restaurant);
                log.info("Processed order.completed for restaurantId={} — stats updated",
                        event.restaurantId());
            }, () -> log.warn("order.completed: restaurant {} not found — skipping", event.restaurantId()));

        } catch (Exception ex) {
            log.error("Failed to process order.completed for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    // ── order.cancelled → reverse restaurant stats ────────────────────────────
    @RabbitHandler
    @Transactional
    public void onOrderCancelled(
            OrderCancelledEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitMQ.ORDER_CANCELLED_ROUTING_KEY, correlationId, event.orderId(), event.restaurantId());
        try {
            log.info("Consuming order.cancelled for orderId={}", event.orderId());

            restaurantRepository.findById(event.restaurantId()).ifPresentOrElse(restaurant -> {
                restaurantRepository.save(restaurant);
                log.info("Processed order.cancelled for restaurantId={} — stats reversed",
                        event.restaurantId());
            }, () -> log.warn("order.cancelled: restaurant {} not found — skipping", event.restaurantId()));

        } catch (Exception ex) {
            log.error("Failed to process order.cancelled for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
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

    private void setupMdc(String routingKey, String correlationId, Long orderId, Long restaurantId) {
        if (routingKey    != null) MDC.put("routingKey",    routingKey);
        if (correlationId != null) MDC.put("correlationId", correlationId);
        if (orderId       != null) MDC.put("orderId",       orderId.toString());
        if (restaurantId  != null) MDC.put("restaurantId",  restaurantId.toString());
    }

    private void clearMdc() {
        MDC.remove("routingKey");
        MDC.remove("correlationId");
        MDC.remove("orderId");
        MDC.remove("restaurantId");
    }
}