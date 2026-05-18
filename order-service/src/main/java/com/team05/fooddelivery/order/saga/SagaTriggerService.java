package com.team05.fooddelivery.order.saga;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import com.team05.fooddelivery.order.service.OrderService;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class SagaTriggerService {
    private Map<Integer, String> pendingPaymentAndDeliveryEvents;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private static final Logger log = LoggerFactory.getLogger(SagaTriggerService.class);

        public SagaTriggerService(OrderService orderService, OrderRepository orderRepository) {
            this.orderService = orderService;
            this.orderRepository = orderRepository;
            this.pendingPaymentAndDeliveryEvents = new HashMap<>();
        }

    @Transactional
    @Caching(
    evict = {
        @CacheEvict(value = "order-service::order", key = "#orderId"),
        @CacheEvict(value = "order-service::S3-F1", allEntries = true),
        @CacheEvict(value = "order-service::S3-F3", allEntries = true),
        @CacheEvict(value = "order-service::S3-F5", allEntries = true),
        @CacheEvict(value = "order-service::S3-F6", allEntries = true),
        @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
        @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true)
    })
    public void processDeliveryCreatedAndPaymentInitiatedEvent(Long orderId, String receivedEvent){
        // Read from the list of pending events
        String existingEvent = pendingPaymentAndDeliveryEvents.get(orderId.hashCode());

        // If no event is there at all, add it and wait for the other event
        if (existingEvent == null) {
            pendingPaymentAndDeliveryEvents.put(orderId.hashCode(), receivedEvent);
            // log.info("Received {} for orderId={}, waiting for the other event", receivedEvent, orderId);
            return;
        }

        // This means the same event was received twice, which should not happen. Ignore.
        if (existingEvent.equals(receivedEvent)) {
            // log.warn("Received duplicate {} for orderId={}, ignoring", receivedEvent, orderId);
            return;
        }

        // If we have both payment and delivery events, we can proceed with the completion of the order
        if ((existingEvent.equals("payment.initiated") && receivedEvent.equals("delivery.created")) ||
            (existingEvent.equals("delivery.created") && receivedEvent.equals("payment.initiated"))) {
                // Proceed with order completion logic, e.g. update
                Order order = orderService.getOrderById(orderId);
                String previousStatus = order.getStatus().toString();
                order.setStatus(OrderStatusEnum.PAYMENT_PENDING);
                orderRepository.save(order);

                log.info("Order {} transitioning {} → {}", orderId, previousStatus, OrderStatusEnum.PAYMENT_PENDING);
                log.info("{} {} saved with status={}", "Order", orderId, OrderStatusEnum.PAYMENT_PENDING);

                // Remove the entry from pending events
                pendingPaymentAndDeliveryEvents.remove(orderId.hashCode());
            }


    }

    @Transactional
    @Caching(
    evict = {
        @CacheEvict(value = "order-service::order", key = "#orderId"),
        @CacheEvict(value = "order-service::S3-F1", allEntries = true),
        @CacheEvict(value = "order-service::S3-F3", allEntries = true),
        @CacheEvict(value = "order-service::S3-F5", allEntries = true),
        @CacheEvict(value = "order-service::S3-F6", allEntries = true),
        @CacheEvict(value = "order-service::S3-F9", key = "#orderId"),
        @CacheEvict(value = "restaurant-service::S2-F12", allEntries = true)

    })
    public void processOtherPaymentEvents(Long orderId, String receivedEvent) {
        Order order = orderService.getOrderById(orderId);
        String previousStatus = order.getStatus().toString();
        if (receivedEvent.equals("payment.completed")) {
            order.setStatus(OrderStatusEnum.PAID);
            orderRepository.save(order);
            log.info("Order {} transitioning {} → {}", orderId, previousStatus, OrderStatusEnum.PAID);
            log.info("{} {} saved with status={}", "Order", orderId, OrderStatusEnum.PAID);
        } else if (receivedEvent.equals("payment.failed")) {
            orderService.cancelOrder(orderId);
            order.setStatus(OrderStatusEnum.PAYMENT_FAILED);
            orderRepository.save(order);
            log.info("Order {} transitioning {} → {}", orderId, previousStatus, OrderStatusEnum.PAYMENT_FAILED);
            log.info("{} {} saved with status={}", "Order", orderId, OrderStatusEnum.PAYMENT_FAILED);
        } else if (receivedEvent.equals("payment.refunded")) {
            order.setStatus(OrderStatusEnum.REFUNDED);
            orderRepository.save(order);
            log.info("Order {} transitioning {} → {}", orderId, previousStatus, OrderStatusEnum.REFUNDED);
            log.info("{} {} saved with status={}", "Order", orderId, OrderStatusEnum.REFUNDED);
        }
    }
}
