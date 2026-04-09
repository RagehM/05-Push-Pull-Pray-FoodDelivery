package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> searchOrders(OrderStatusEnum status, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        return orderRepository.searchByStatusAndDateRange(
                status,
                startDateTime,
                endDateTimeExclusive
        );
    }
    // [CRUD]
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order createOrder(Order order) {
        boolean userExists = orderRepository.existsByUserId(order.getUserId());
        if (!userExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }

        boolean restaurantExists = orderRepository.existsByRestaurantId(order.getRestaurantId());
        if (!restaurantExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant not found");
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(Long orderId, Order updatedOrder) {
        Order existingOrder = getOrderById(orderId);

        if (updatedOrder.getDeliveryAddress() != null) {
            existingOrder.setDeliveryAddress(updatedOrder.getDeliveryAddress());
        }

        if (updatedOrder.getTotalAmount() != null) {
            existingOrder.setTotalAmount(updatedOrder.getTotalAmount());
        }

        if (updatedOrder.getMetadata() != null) {
            existingOrder.setMetadata(updatedOrder.getMetadata());
        }

        if (updatedOrder.getStatus() != null) {
            existingOrder.setStatus(updatedOrder.getStatus());
        }

        if (updatedOrder.getDeliveredAt() != null) {
            existingOrder.setDeliveredAt(updatedOrder.getDeliveredAt());
        }

        return orderRepository.save(existingOrder);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order existingOrder = getOrderById(orderId);
        orderRepository.delete(existingOrder);
    }

    @Transactional(readOnly = true)
    public List<Order> searchOrdersByMetadata(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata key must not be empty");
        }

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata value must not be empty");
        }

        return orderRepository.findByMetadataKeyValue(key.trim(), value.trim());
    }
}