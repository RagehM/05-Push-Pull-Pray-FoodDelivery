package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // [CRUD]
    //// Get order by ID
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
    //// Get all orders
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        List<Order> allOrders = orderRepository.findAll();
        return allOrders;
    }
    //// Create order
    @Transactional
    public Order createOrder(Order order) {
        // boolean userExists = orderRepository.existsByUserId(order.getUserId());
        // if (!userExists) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        // }
        // boolean restaurantExists = orderRepository.existsByRestaurantId(order.getRestaurantId());
        // if (!restaurantExists) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant not found");
        // }
        return orderRepository.save(order);
    }
    //// Update order
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
    //// Delete order
    @Transactional
    public void deleteOrder(Long orderId) {
        Order existingOrder = getOrderById(orderId);
        orderRepository.delete(existingOrder);
    }
}