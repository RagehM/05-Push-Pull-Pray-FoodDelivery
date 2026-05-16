package com.team05.fooddelivery.order.saga;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import com.team05.fooddelivery.order.service.OrderService;

import jakarta.transaction.Transactional;


@Service
public class SagaTriggerService {
    private Map<Integer, String> pendingPaymentAndDeliveryEvents;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

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
                order.setStatus(OrderStatusEnum.PAYMENT_PENDING);
                orderRepository.save(order);

                // Remove the entry from pending events
                pendingPaymentAndDeliveryEvents.remove(orderId.hashCode());
            }


    }
}
