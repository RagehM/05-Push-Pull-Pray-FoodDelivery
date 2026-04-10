package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.dto.OrderDetailsDTO;
import com.team05.fooddelivery.order.dto.OrderItemDetailsDTO;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;



import org.springframework.http.HttpStatus;
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

    // [S3-F6] - Order Analytics by Time Period (Report DTO)
    public OrderAnalyticsDTO getOrderAnalyticsByTimePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.getOrderAnalyticsByTimePeriod(startDate, endDate);
    }

    @Transactional
    // [S3-F8] Add items to existing order
    public Order addItemsToOrder(Long orderId, List<OrderItem> orderItems) {
        Order existingOrder = getOrderById(orderId);
        int line_item_count = existingOrder.getOrderItems() != null ? existingOrder.getOrderItems().size() : 0;
        if (!(existingOrder.getStatus() == OrderStatusEnum.PLACED || existingOrder.getStatus() == OrderStatusEnum.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only add items to orders that are in PLACED or CONFIRMED status");
        }
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getQuantity() == null || orderItem.getItemName() == null || orderItem.getUnitPrice() == null || orderItem.getMenuItemId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order item must have quantity, item name, unit price and menu item id");
            }
            orderItem.setLineNumber(++line_item_count);
            orderItem.setStatus(OrderItemStatusEnum.PENDING);
            orderItem.setOrder(existingOrder);
        }
        existingOrder.getOrderItems().addAll(orderItems);
        orderRepository.save(existingOrder);

        Order returnObject = orderRepository.getOrderWithOrderItemsById(orderId);

        return returnObject;
    }

    @Transactional(readOnly = true)
    public List<Order> searchOrdersByMetadata(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata key must not be empty");
        }

        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Metadata value must not be empty");
        }

        return orderRepository.findByMetadataKeyValue(key.trim(), value.trim());
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