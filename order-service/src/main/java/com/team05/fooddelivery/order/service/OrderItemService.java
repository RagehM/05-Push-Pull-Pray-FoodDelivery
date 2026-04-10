package com.team05.fooddelivery.order.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.repository.OrderItemRepository;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    public OrderItemService(OrderItemRepository orderItemRepository, OrderService orderService) {
        this.orderItemRepository = orderItemRepository;
        this.orderService = orderService;
    }

    public OrderItem getOrderItemById(Long id) {
        return orderItemRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OrderItem not found with id: " + id));
    }

    public List<OrderItem> getAllOrderItems() {
        List<OrderItem> allOrderItems = orderItemRepository.findAll();
        return allOrderItems;
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No order items found for order id: " + orderId);
        }
        return orderItems;
    }

    @Transactional
    public OrderItem createOrderItem(OrderItem orderItem) {
        Order order = orderService.getOrderById(orderItem.getOrder().getId());
//        String itemName = orderItemRepository.getMenuItemName(orderItem.getMenuItemId());
//        if (itemName == null) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item not found with id: " + orderItem.getMenuItemId());
//        }
        orderItem.setOrder(order);
    //    orderItem.setItemName(itemName);
        return orderItemRepository.save(orderItem);
    }

    @Transactional
    public OrderItem updateOrderItem(Long id, OrderItem orderItem) {
        OrderItem existingOrderItem = getOrderItemById(id);

        if (orderItem.getLineNumber() != null) {
            existingOrderItem.setLineNumber(orderItem.getLineNumber());
        }

        if (orderItem.getMenuItemId() != null) {
            String itemName = orderItemRepository.getMenuItemName(orderItem.getMenuItemId());
            if (itemName == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item not found with id: " + orderItem.getMenuItemId());
            }
            existingOrderItem.setMenuItemId(orderItem.getMenuItemId());
            existingOrderItem.setItemName(itemName);
        }

        if (orderItem.getQuantity() != null) {
            if (orderItem.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
            }
            existingOrderItem.setQuantity(orderItem.getQuantity());
        }

        if (orderItem.getUnitPrice() != null) {
            existingOrderItem.setUnitPrice(orderItem.getUnitPrice());
        }

        if (orderItem.getStatus() != null) {
            existingOrderItem.setStatus(orderItem.getStatus());
        }

        if (orderItem.getMetadata() != null) {
            existingOrderItem.setMetadata(orderItem.getMetadata());
        }

        return orderItemRepository.save(existingOrderItem);
    }

    @Transactional
    public void deleteOrderItem(Long id) {
        OrderItem existingOrderItem = getOrderItemById(id);
        orderItemRepository.delete(existingOrderItem);
    }
    
}
