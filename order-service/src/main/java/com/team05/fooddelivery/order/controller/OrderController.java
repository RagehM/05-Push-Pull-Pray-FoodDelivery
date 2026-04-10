package com.team05.fooddelivery.order.controller;

import com.team05.fooddelivery.order.dto.OrderDetailsDTO;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;



@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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

    @GetMapping("/{orderId}/details")
    public ResponseEntity<OrderDetailsDTO> getOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetails(orderId));
    }

        // [S3-F6] - Order Analytics by Time Period (Report DTO)
    @GetMapping("/analytics")
    public OrderAnalyticsDTO getOrderAnalyticsByTimePeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return orderService.getOrderAnalyticsByTimePeriod(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    // [S3-F8] Add items to existing order
    @PostMapping("/{orderId}/items")
    public Order addItemsToOrder(@PathVariable Long orderId, @RequestBody java.util.List<OrderItem> orderItems) {
        return orderService.addItemsToOrder(orderId, orderItems);
    }
}