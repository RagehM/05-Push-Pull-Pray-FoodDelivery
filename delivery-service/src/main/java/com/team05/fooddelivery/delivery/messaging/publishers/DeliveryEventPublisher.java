package com.team05.fooddelivery.delivery.messaging.publishers;

import com.team05.fooddelivery.contracts.events.DeliveryCancelledEvent;
import com.team05.fooddelivery.contracts.events.DeliveryCreatedEvent;
import com.team05.fooddelivery.contracts.events.DeliveryStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventPublisher.class);
    private static final String EXCHANGE = "delivery.events";

    private final RabbitTemplate rabbitTemplate;

    public DeliveryEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDeliveryCreated(DeliveryCreatedEvent event) {
        String routingKey = "delivery.created";
        MDC.put("routingKey", routingKey);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for {}={}", routingKey, "Delivery", event.deliveryId());
        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage());
        } finally {
            MDC.remove("routingKey");
        }
    }

    public void publishDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        String routingKey = "delivery.status.changed";
        MDC.put("routingKey", routingKey);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for {}={}", routingKey, "Delivery", event.deliveryId());
        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage());
        } finally {
            MDC.remove("routingKey");
        }
    }

    public void publishDeliveryCancelled(DeliveryCancelledEvent event) {
        String routingKey = "delivery.cancelled";
        MDC.put("routingKey", routingKey);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for {}={}", routingKey, "Delivery", event.deliveryId());
        } catch (Exception ex) {
            log.error("Failed to process {}: {}", routingKey, ex.getMessage());
        } finally {
            MDC.remove("routingKey");
        }
    }
}