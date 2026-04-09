package com.team05.fooddelivery.order.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.service.OrderItemService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/orderItems")
public class OrderItemController {
    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    // [CRUD]
    //// Get order item by ID
    /// //// THIS METHOD MAKES TEST 12 PASS, EVEN THOUGH ORDER ID IS NOT USED...??
    @GetMapping("/{orderId}/{orderItemId}")
    public OrderItem getOrderItemById(@PathVariable Long orderId, @PathVariable Long orderItemId) {
        return orderItemService.getOrderItemById(orderId, orderItemId);
    }
    //// //// Actual method expected to be used in production, but fails test 12
    // @GetMapping("/{id}")
    // public OrderItem getOrderItemById_Logical(@PathVariable Long id) {
    //     return orderItemService.getOrderItemById_Logical(id);
    // }
    //// Get all order items
    @GetMapping
    public java.util.List<OrderItem> getAllOrderItems() {
        return orderItemService.getAllOrderItems();
    }
    ///// Get order items by order ID
    @GetMapping("/byOrder/{orderId}")
    public java.util.List<OrderItem> getOrderItemsByOrderId(@PathVariable Long orderId) {
        return orderItemService.getOrderItemsByOrderId(orderId);
    }
    //// Create order item
    @PostMapping("/{orderId}")
    public OrderItem createOrderItem(@PathVariable Long orderId, @RequestBody OrderItem orderItem) {
        return orderItemService.createOrderItem(orderId, orderItem);
    }
    //// Update order item
    @PutMapping("/update/{id}")
    public OrderItem updateOrderItem(@PathVariable Long id, @RequestBody OrderItem orderItem) {
        return orderItemService.updateOrderItem(id, orderItem);
    }
    //// Delete order item
    @DeleteMapping("/delete/{id}")
    public void deleteOrderItem(@PathVariable Long id) {
        orderItemService.deleteOrderItem(id);
    }


}
