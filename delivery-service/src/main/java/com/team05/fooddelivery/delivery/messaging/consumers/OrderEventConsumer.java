package com.team05.fooddelivery.delivery.messaging.consumers;

import com.team05.fooddelivery.contracts.events.DeliveryCancelledEvent;
import com.team05.fooddelivery.contracts.events.DeliveryCreatedEvent;
import com.team05.fooddelivery.contracts.events.OrderCancelledEvent;
import com.team05.fooddelivery.contracts.events.OrderCompletedEvent;
import com.team05.fooddelivery.contracts.events.OrderPlacedEvent;
import com.team05.fooddelivery.contracts.feign.RestaurantServiceClient;
import com.team05.fooddelivery.delivery.enums.DeliveryStatus;
import com.team05.fooddelivery.delivery.messaging.publishers.DeliveryEventPublisher;
import com.team05.fooddelivery.delivery.model.Delivery;
import com.team05.fooddelivery.delivery.repository.DeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RabbitListener(queues = OrderEventConsumer.QUEUE_NAME, containerFactory = "rabbitListenerContainerFactory")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final DeliveryRepository deliveryRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final DeliveryEventPublisher deliveryEventPublisher;

    public static final String QUEUE_NAME = "delivery.saga-listener";
    private static final String ROUTING_ORDER_PLACED = "order.placed";
    private static final String ROUTING_ORDER_COMPLETED = "order.completed";
    private static final String ROUTING_ORDER_CANCELLED = "order.cancelled";

    private static final String MDC_ROUTING_KEY = "routingKey";
    private static final String MDC_ORDER_ID = "orderId";
    private static final String MDC_DELIVERY_ID = "deliveryId";

    private static final String MSG_CONSUMING = "Consuming {} for {}={}";
    private static final String MSG_PROCESSED = "Processed {} for {}={}";
    private static final String MSG_FAILED = "Failed to process {}: {}";
    private static final String MSG_CALLING = "Calling {}.{} with args={}";
    private static final String MSG_RETURNED = "{}.{} returned successfully";

    public OrderEventConsumer(
            DeliveryRepository deliveryRepository,
            RestaurantServiceClient restaurantServiceClient,
            DeliveryEventPublisher deliveryEventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.restaurantServiceClient = restaurantServiceClient;
        this.deliveryEventPublisher = deliveryEventPublisher;
    }

    // Minimal handler for order.placed — infrastructure required by the spec
    @RabbitHandler
    public void onOrderPlaced(OrderPlacedEvent event) {
        MDC.put(MDC_ROUTING_KEY, ROUTING_ORDER_PLACED);
        MDC.put(MDC_ORDER_ID, String.valueOf(event.orderId()));
        try {
            log.info(MSG_CONSUMING, ROUTING_ORDER_PLACED, MDC_ORDER_ID, event.orderId());
            // minimal body — maybe increment a metric or log
            log.info(MSG_PROCESSED, ROUTING_ORDER_PLACED, MDC_ORDER_ID, event.orderId());
        } catch (Exception ex) {
            log.error(MSG_FAILED, ROUTING_ORDER_PLACED, ex.getMessage());
            throw ex;
        } finally {
            MDC.remove(MDC_ROUTING_KEY);
            MDC.remove(MDC_ORDER_ID);
        }
    }

    // Handler for order.completed
    @RabbitHandler
    public void onOrderCompleted(OrderCompletedEvent event) {
        MDC.put(MDC_ROUTING_KEY, ROUTING_ORDER_COMPLETED);
        MDC.put(MDC_ORDER_ID, String.valueOf(event.orderId()));
        try {
            log.info(MSG_CONSUMING, ROUTING_ORDER_COMPLETED, MDC_ORDER_ID, event.orderId());

            // find latest delivery for this order
            Optional<Delivery> opt = deliveryRepository.findLatestByOrderId(event.orderId());
            if (opt.isEmpty()) {
                log.info("No delivery row for {}={} — skipping", MDC_ORDER_ID, event.orderId());
                return;
            }

            Delivery delivery = opt.get();
            // Only proceed if status is ASSIGNED|PICKED_UP|IN_TRANSIT
            if (!(delivery.getStatus() == DeliveryStatus.ASSIGNED
                    || delivery.getStatus() == DeliveryStatus.PICKED_UP
                    || delivery.getStatus() == DeliveryStatus.IN_TRANSIT)) {

                log.info("Delivery {} for {}={} is in status {} — skipping", delivery.getId(), MDC_ORDER_ID, event.orderId(), delivery.getStatus());
                return;
            }

            MDC.put(MDC_DELIVERY_ID, String.valueOf(delivery.getId()));

            // Fetch restaurant name via Feign
            log.info(MSG_CALLING, "RestaurantServiceClient", "getRestaurant", event.restaurantId());
            var restaurant = restaurantServiceClient.getRestaurant(event.restaurantId());
            log.info(MSG_RETURNED, "RestaurantServiceClient", "getRestaurant");

            // finalize delivery
            delivery.setStatus(DeliveryStatus.DELIVERED);
            delivery.setCompletedAt(LocalDateTime.now());
            // Save restaurant name into delivery model if present
            try {
                delivery.setRestaurantName(restaurant.name());
            } catch (Exception ignored) {
            }
            deliveryRepository.save(delivery);
            log.info("Delivery {} updated to DELIVERED", delivery.getId());

            // Publish delivery.created
            // NOTE: contracts.DeliveryCreatedEvent may have 'driverName' or 'restaurantName' field;
            // Fill the event according to your contracts module shape.
            // If the contract expects restaurantName (PDF), use:
            // new DeliveryCreatedEvent(delivery.getId(), event.orderId(), event.restaurantId(), restaurant.name())
            // If the contract still has 'driverName' (legacy), pass restaurant.name() into that slot (syntax-only).
            DeliveryCreatedEvent createdEvent = new DeliveryCreatedEvent(
                    delivery.getId(),
                    event.orderId(),
                    event.restaurantId(),
                    restaurant.name() // maps into the record's last field
            );
            deliveryEventPublisher.publishDeliveryCreated(createdEvent);

            log.info(MSG_PROCESSED, ROUTING_ORDER_COMPLETED, MDC_ORDER_ID, event.orderId());
        } catch (Exception ex) {
            log.error(MSG_FAILED, ROUTING_ORDER_COMPLETED, ex.getMessage());
            throw ex;
        } finally {
            MDC.remove(MDC_ROUTING_KEY);
            MDC.remove(MDC_ORDER_ID);
            MDC.remove(MDC_DELIVERY_ID);
        }
    }

    // Handler for order.cancelled
    @RabbitHandler
    public void onOrderCancelled(OrderCancelledEvent event) {
        MDC.put(MDC_ROUTING_KEY, ROUTING_ORDER_CANCELLED);
        MDC.put(MDC_ORDER_ID, String.valueOf(event.orderId()));
        try {
            log.info(MSG_CONSUMING, ROUTING_ORDER_CANCELLED, MDC_ORDER_ID, event.orderId());

            Optional<Delivery> opt = deliveryRepository.findLatestByOrderId(event.orderId());
            if (opt.isEmpty()) {
                log.info("No delivery row for {}={} — nothing to cancel", MDC_ORDER_ID, event.orderId());
                return;
            }

            Delivery delivery = opt.get();
            if (!(delivery.getStatus() == DeliveryStatus.ASSIGNED
                    || delivery.getStatus() == DeliveryStatus.PICKED_UP
                    || delivery.getStatus() == DeliveryStatus.IN_TRANSIT)) {
                log.info("Delivery {} for {}={} is in status {} — skipping cancel", delivery.getId(), MDC_ORDER_ID, event.orderId(), delivery.getStatus());
                return;
            }

            MDC.put(MDC_DELIVERY_ID, String.valueOf(delivery.getId()));

            delivery.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(delivery);
            log.info("Delivery {} updated to CANCELLED", delivery.getId());

            DeliveryCancelledEvent cancelledEvent = new DeliveryCancelledEvent(delivery.getId(), event.orderId());
            deliveryEventPublisher.publishDeliveryCancelled(cancelledEvent);

            log.info(MSG_PROCESSED, ROUTING_ORDER_CANCELLED, MDC_ORDER_ID, event.orderId());
        } catch (Exception ex) {
            log.error(MSG_FAILED, ROUTING_ORDER_CANCELLED, ex.getMessage());
            throw ex;
        } finally {
            MDC.remove(MDC_ROUTING_KEY);
            MDC.remove(MDC_ORDER_ID);
            MDC.remove(MDC_DELIVERY_ID);
        }
    }

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object unknown) {
        log.warn("Received unknown message type: {}",
                unknown == null ? "null" : unknown.getClass().getName());
    }
}