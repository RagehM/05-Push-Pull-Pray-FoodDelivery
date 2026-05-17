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

    private static final String ROUTING_DELIVERY_CREATED = "delivery.created";
    private static final String ROUTING_DELIVERY_STATUS_CHANGED = "delivery.status.changed";
    private static final String ROUTING_DELIVERY_CANCELLED = "delivery.cancelled";

    private static final String MDC_ROUTING_KEY = "routingKey";
    private static final String MDC_ORDER_ID = "orderId";
    private static final String MDC_DELIVERY_ID = "deliveryId";

    private static final String MSG_PUBLISHED = "Published {} for {}={}";
    private static final String MSG_FAILED = "Failed to process {}: {}";

    private final RabbitTemplate rabbitTemplate;

    public DeliveryEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDeliveryCreated(DeliveryCreatedEvent event) {
        String routingKey = ROUTING_DELIVERY_CREATED;
        MDC.put(MDC_ROUTING_KEY, routingKey);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info(MSG_PUBLISHED, routingKey, "Delivery", event.deliveryId());
        } catch (Exception ex) {
            log.error(MSG_FAILED, routingKey, ex.getMessage());
        } finally {
            MDC.remove(MDC_ROUTING_KEY);
        }
    }

    public void publishDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        String routingKey = ROUTING_DELIVERY_STATUS_CHANGED;
        MDC.put(MDC_ROUTING_KEY, routingKey);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info(MSG_PUBLISHED, routingKey, "Delivery", event.deliveryId());
        } catch (Exception ex) {
            log.error(MSG_FAILED, routingKey, ex.getMessage());
        } finally {
            MDC.remove(MDC_ROUTING_KEY);
        }
    }

    public void publishDeliveryCancelled(DeliveryCancelledEvent event) {
        String routingKey = ROUTING_DELIVERY_CANCELLED;
        MDC.put(MDC_ROUTING_KEY, routingKey);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info(MSG_PUBLISHED, routingKey, "Delivery", event.deliveryId());
        } catch (Exception ex) {
            log.error(MSG_FAILED, routingKey, ex.getMessage());
        } finally {
            MDC.remove(MDC_ROUTING_KEY);
        }
    }
}