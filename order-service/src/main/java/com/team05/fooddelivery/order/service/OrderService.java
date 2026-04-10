package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.dto.OrderDetailsDTO;
import com.team05.fooddelivery.order.dto.OrderItemDetailsDTO;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.repository.OrderRepository;

import java.util.ArrayList;
import java.util.Comparator;
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
//        boolean userExists = orderRepository.existsByUserId(order.getUserId());
//        if (!userExists) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
//        }
//
//        boolean restaurantExists = orderRepository.existsByRestaurantId(order.getRestaurantId());
//        if (!restaurantExists) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant not found");
//        }

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

    // [S3-F9]
    @Transactional(readOnly = true)
    public OrderDetailsDTO getOrderDetails(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        List<OrderItem> orderItems = order.getOrderItems() != null
                ? new ArrayList<>(order.getOrderItems())
                : new ArrayList<>();

        orderItems.sort(Comparator.comparing(OrderItem::getLineNumber));

        List<OrderItemDetailsDTO> items = orderItems.stream()
                .map(item -> new OrderItemDetailsDTO(
                        item.getId(),
                        item.getLineNumber(),
                        item.getItemName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getStatus(),
                        item.getMetadata()
                ))
                .toList();

        int totalItems = items.size();

        int preparedItems = (int) orderItems.stream()
                .filter(item -> item.getStatus() == OrderItemStatusEnum.PREPARED)
                .count();

        return new OrderDetailsDTO(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getMetadata(),
                items,
                totalItems,
                preparedItems
        );
    }
}