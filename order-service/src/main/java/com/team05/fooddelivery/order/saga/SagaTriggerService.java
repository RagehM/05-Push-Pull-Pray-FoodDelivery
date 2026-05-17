package com.team05.fooddelivery.order.saga;

import java.util.HashMap;
import java.util.Map;

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
    public void processDeliveryCreatedAndPaymentInitiatedEvent(Long orderId, String receivedEvent){
        // Read from the list of pending events
        String existingEvent = pendingPaymentAndDeliveryEvents.get(orderId.hashCode());

        // If no event is there at all, add it and wait for the other event
        if (existingEvent == null) {
            pendingPaymentAndDeliveryEvents.put(orderId.hashCode(), receivedEvent);
            return;
        }

        // This means the same event was received twice, which should not happen. Ignore.
        if (existingEvent.equals(receivedEvent)) {
            return;
        }

        // If we have both payment and delivery events, we can proceed with the completion of the order
        if ((existingEvent.equals("payment.completed") && receivedEvent.equals("delivery.created")) ||
            (existingEvent.equals("delivery.created") && receivedEvent.equals("payment.completed"))) {
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
    public void processOtherPaymentEvents(Long orderId, String receivedEvent) {
        Order order = orderService.getOrderById(orderId);
        String previousStatus = order.getStatus().toString();
        if (receivedEvent.equals("payment.completed")) {
            order.setStatus(OrderStatusEnum.PAID);
            orderRepository.save(order);
            log.info("Order {} transitioning {} → {}", orderId, previousStatus, OrderStatusEnum.PAID);
            log.info("{} {} saved with status={}", "Order", orderId, OrderStatusEnum.PAID);
        } else if (receivedEvent.equals("payment.failed")) {
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
