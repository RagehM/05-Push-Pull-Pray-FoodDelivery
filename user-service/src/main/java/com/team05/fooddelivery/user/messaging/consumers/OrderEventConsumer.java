package com.team05.fooddelivery.user.messaging.consumers;

import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.user.config.RabbitConfig;
import com.team05.fooddelivery.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitConfig.USER_SAGA_LISTENER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final UserService userService;

    public OrderEventConsumer(UserService userService) {
        this.userService = userService;
    }

    /**
     * order.completed → increment totalOrders by 1 and add totalAmount to totalSpent.
     */
    @RabbitHandler
    public void onOrderCompleted(
            OrderCompletedEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitConfig.ORDER_COMPLETED_ROUTING_KEY, correlationId, event.orderId());
        try {
            log.info("Consuming order.completed for orderId={}, userId={}", event.orderId(), event.userId());
            userService.incrementUserOrderStats(event.userId(), event.totalAmount());
            log.info("Processed order.completed for orderId={} — updated stats for userId={}",
                    event.orderId(), event.userId());
        } catch (Exception ex) {
            log.error("Failed to process order.completed for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
            throw ex; // NACK → retry → DLQ
        } finally {
            clearMdc();
        }
    }

    /**
     * order.cancelled → decrement totalOrders by 1 and subtract the order amount from totalSpent.
     * The order amount is fetched from order-service inside UserService.
     */
    @RabbitHandler
    public void onOrderCancelled(
            OrderCancelledEvent event,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        setupMdc(RabbitConfig.ORDER_CANCELLED_ROUTING_KEY, correlationId, event.orderId());
        try {
            log.info("Consuming order.cancelled for orderId={}, userId={}", event.orderId(), event.userId());
            userService.decrementUserOrderStats(event.userId(), event.orderId());
            log.info("Processed order.cancelled for orderId={} — reversed stats for userId={}", event.orderId(), event.userId());
        } catch (Exception ex) {
            log.error("Failed to process order.cancelled for orderId={}: {}",
                    event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            clearMdc();
        }
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object unknown) {
        log.warn("user.order.saga-listener received unknown message type: {}",
                unknown == null ? "null" : unknown.getClass().getName());
    }

    private void setupMdc(String routingKey, String correlationId, Long orderId) {
        if (routingKey    != null) MDC.put("routingKey",    routingKey);
        if (correlationId != null) MDC.put("correlationId", correlationId);
        if (orderId       != null) MDC.put("orderId",       orderId.toString());
    }

    private void clearMdc() {
        MDC.remove("routingKey");
        MDC.remove("correlationId");
        MDC.remove("orderId");
        MDC.remove("userId");
    }
}
