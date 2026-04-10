package com.team05.fooddelivery.order.controller;

import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import com.team05.fooddelivery.order.model.Order;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;



@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // [S3-F5]
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Order>> searchOrdersByMetadata(
            @RequestParam String key,
            @RequestParam String value) {

        List<Order> orders = orderService.searchOrdersByMetadata(key, value);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Order>> searchOrders(
            @RequestParam(required = false) OrderStatusEnum status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(orderService.searchOrders(status, startDate, endDate));
    }
    @PutMapping("{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        orderService.cancelOrder(Long.parseLong(id));
        return ResponseEntity.ok().build();
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
    @PutMapping("/{id}")
    public Order updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.updateOrder(id, order);
    }
    //// Delete order
    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }
    //// Deliver order
    @PutMapping("/{id}/deliver")
    public Order deliverOrder(@PathVariable Long id) {
        return orderService.deliverOrder(id);
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