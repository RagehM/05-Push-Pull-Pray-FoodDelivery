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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final DeliveryRepository deliveryRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final DeliveryEventPublisher deliveryEventPublisher;

    public OrderEventConsumer(
            DeliveryRepository deliveryRepository,
            RestaurantServiceClient restaurantServiceClient,
            DeliveryEventPublisher deliveryEventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.restaurantServiceClient = restaurantServiceClient;
        this.deliveryEventPublisher = deliveryEventPublisher;
    }

    // Minimal handler for order.placed — infrastructure required by the spec
    @RabbitListener(queues = "delivery.saga-listener")
    public void onOrderPlaced(OrderPlacedEvent event) {
        MDC.put("routingKey", "order.placed");
        MDC.put("orderId", String.valueOf(event.orderId()));
        try {
            log.info("consuming order.placed for orderId={}", event.orderId());
            // minimal body — maybe increment a metric or log
            log.info("processed order.placed for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("error processing order.placed for orderId={} - {}", event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("routingKey");
            MDC.remove("orderId");
        }
    }

    // Handler for order.completed
    @RabbitListener(queues = "delivery.saga-listener")
    public void onOrderCompleted(OrderCompletedEvent event) {
        MDC.put("routingKey", "order.completed");
        MDC.put("orderId", String.valueOf(event.orderId()));
        try {
            log.info("consuming order.completed for orderId={}", event.orderId());

            // find latest delivery for this order
            Optional<Delivery> opt = deliveryRepository.findLatestByOrderId(event.orderId());
            if (opt.isEmpty()) {
                log.info("No delivery row for orderId={} — skipping", event.orderId());
                return;
            }

            Delivery delivery = opt.get();
            // Only proceed if status is ASSIGNED|PICKED_UP|IN_TRANSIT
            if (!(delivery.getStatus() == DeliveryStatus.ASSIGNED
                    || delivery.getStatus() == DeliveryStatus.PICKED_UP
                    || delivery.getStatus() == DeliveryStatus.IN_TRANSIT)) {

                log.info("Delivery {} for orderId={} is in status {} — skipping", delivery.getId(), event.orderId(), delivery.getStatus());
                return;
            }

            MDC.put("deliveryId", String.valueOf(delivery.getId()));

            // Fetch restaurant name via Feign
            log.info("Calling RestaurantServiceClient.getRestaurant with id={}", event.restaurantId());
            var restaurant = restaurantServiceClient.getRestaurant(event.restaurantId());
            log.info("RestaurantServiceClient returned for id={}", event.restaurantId());

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

            log.info("Processed order.completed for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("error processing order.completed for orderId={} - {}", event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("routingKey");
            MDC.remove("orderId");
            MDC.remove("deliveryId");
        }
    }

    // Handler for order.cancelled
    @RabbitListener(queues = "delivery.saga-listener")
    public void onOrderCancelled(OrderCancelledEvent event) {
        MDC.put("routingKey", "order.cancelled");
        MDC.put("orderId", String.valueOf(event.orderId()));
        try {
            log.info("consuming order.cancelled for orderId={}", event.orderId());

            Optional<Delivery> opt = deliveryRepository.findLatestByOrderId(event.orderId());
            if (opt.isEmpty()) {
                log.info("No delivery row for orderId={} — nothing to cancel", event.orderId());
                return;
            }

            Delivery delivery = opt.get();
            if (!(delivery.getStatus() == DeliveryStatus.ASSIGNED
                    || delivery.getStatus() == DeliveryStatus.PICKED_UP
                    || delivery.getStatus() == DeliveryStatus.IN_TRANSIT)) {
                log.info("Delivery {} for orderId={} is in status {} — skipping cancel", delivery.getId(), event.orderId(), delivery.getStatus());
                return;
            }

            MDC.put("deliveryId", String.valueOf(delivery.getId()));

            delivery.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.save(delivery);
            log.info("Delivery {} updated to CANCELLED", delivery.getId());

            DeliveryCancelledEvent cancelledEvent = new DeliveryCancelledEvent(delivery.getId(), event.orderId());
            deliveryEventPublisher.publishDeliveryCancelled(cancelledEvent);

            log.info("Processed order.cancelled for orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("error processing order.cancelled for orderId={} - {}", event.orderId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("routingKey");
            MDC.remove("orderId");
            MDC.remove("deliveryId");
        }
    }
}