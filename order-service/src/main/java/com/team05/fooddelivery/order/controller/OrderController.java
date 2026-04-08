package com.team05.fooddelivery.order.controller;

import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/metadata/search")
    public ResponseEntity<List<Order>> searchOrdersByMetadata(
            @RequestParam String key,
            @RequestParam String value) {

        List<Order> orders = orderService.searchOrdersByMetadata(key, value);
        return ResponseEntity.ok(orders);
    }
    // [CRUD]
    //// Get order by ID
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
    //// Get all orders
    @GetMapping
    public java.util.List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }
    //// Create order
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
    //// Update order
    @PutMapping("/update/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }
    //// Delete order
    @DeleteMapping("/delete/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

}