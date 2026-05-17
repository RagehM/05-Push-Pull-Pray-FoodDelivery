package com.team05.fooddelivery.order.controller;

import com.team05.fooddelivery.contracts.dto.*;
import com.team05.fooddelivery.order.dto.*;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.OrderItem;
import com.team05.fooddelivery.order.service.OrderService;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;




@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // [S3-F1]
    @GetMapping("/search")
    public ResponseEntity<List<Order>> searchOrders(
            @RequestParam(required = false) OrderStatusEnum status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Received {} {}", "GET", "/api/orders/search");
        ResponseEntity<List<Order>> returnValue = ResponseEntity.ok(orderService.searchOrders(status, startDate, endDate));
        log.info("Returning {} for {} {}", returnValue, "GET", "/api/orders/search");
        return returnValue;
    }
    // [S3-F2]Confirm order and Assign Resturant
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<Order> confirmOrder(@PathVariable Long orderId, @RequestParam Long restaurantId) {
        log.info("Received {} {}", "PUT", "/api/orders/" + orderId + "/confirm");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Order returnValue = orderService.confirmOrderAndAssignRestaurant(orderId, restaurantId);
        stopWatch.stop();

        log.info("Returning {} for {} {}", returnValue, "PUT", "/api/orders/" + orderId + "/confirm");

        if (stopWatch.getTime() > 1500) {
            log.warn("Slow {} took {} ms", "confirmOrderAndAssignRestaurant", stopWatch.getTime());
        }
        return ResponseEntity.ok(returnValue);
    }
    // [S3-F3] Estimate order cost
    @PostMapping("/estimate")
    public ResponseEntity<OrderCostEstimateDTO> estimateOrder(@RequestBody OrderEstimateRequest request) {
        log.info("Received {} {}", "POST", "/api/orders/estimate");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        OrderCostEstimateDTO returnValue = orderService.estimateOrderCost(request);

        stopWatch.stop();

        log.info("Returning {} for {} {}", returnValue, "POST", "/api/orders/estimate");

        if (stopWatch.getTime() > 1000) {
            log.warn("Slow {} took {} ms", "estimateOrderCost", stopWatch.getTime());
        }

        return ResponseEntity.ok(returnValue);
    }
    // [S3-F4] Deliver order
    @PutMapping("/{id}/deliver")
    public ResponseEntity<Order> deliverOrder(@PathVariable Long id) {
        log.info("Received {} {}", "PUT", "/api/orders/" + id + "/deliver");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Order returnValue = orderService.deliverOrderWrapper(id);
        stopWatch.stop();
        log.info("Returning {} for {} {}", returnValue, "PUT", "/api/orders/" + id + "/deliver");
        if (stopWatch.getTime() > 1500) {
            log.warn("Slow {} took {} ms", "deliverOrder", stopWatch.getTime());
        }
        return ResponseEntity.ok(returnValue);
    }
    // [S3-F5]
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Order>> searchOrdersByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        log.info("Received {} {}", "GET", "/api/orders/metadata/search");
        List<Order> orders = orderService.searchOrdersByMetadata(key, value);
        log.info("Returning {} for {} {}", orders, "GET", "/api/orders/metadata/search");
        return ResponseEntity.ok(orders);
    }
    // [S3-F6] - Order Analytics by Time Period (Report DTO)
    @GetMapping("/analytics")
    public OrderAnalyticsDTO getOrderAnalyticsByTimePeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.info("Received {} {}", "GET", "/api/orders/analytics");
        OrderAnalyticsDTO returnValue = orderService.getOrderAnalyticsByTimePeriod(startDate, endDate);
        stopWatch.stop();

        log.info("Returning {} for {} {}", returnValue, "GET", "/api/orders/analytics");

        if (stopWatch.getTime() > 1000) {
            log.warn("Slow {} took {} ms", "getOrderAnalyticsByTimePeriod", stopWatch.getTime());
        }

        return returnValue;
    }
    // [S3-F7] Cancel Order
    @PutMapping("{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        log.info("Received {} {}", "PUT", "/api/orders/" + id + "/cancel");
        orderService.cancelOrder(Long.parseLong(id));
        log.info("Returning {} for {} {}", "OK", "PUT", "/api/orders/" + id + "/cancel");
        return ResponseEntity.ok().build();
    }
    // [S3-F8] Add items to existing order
    @PostMapping("/{orderId}/items")
    public ResponseEntity<Order> addItemsToOrder(@PathVariable Long orderId, @RequestBody java.util.List<OrderItem> orderItems) {
        log.info("Received {} {}", "POST", "/api/orders/" + orderId + "/items");
        Order returnValue = orderService.addItemsToOrder(orderId, orderItems);
        log.info("Returning {} for {} {}", returnValue, "POST", "/api/orders/" + orderId + "/items");
        return ResponseEntity.ok(returnValue);
    }
    // [S3-F9] Get Order Details (Report DTO)
    @GetMapping("/{orderId}/details")
    public ResponseEntity<OrderDetailsDTO> getOrderDetails(@PathVariable Long orderId) {
        log.info("Received {} {}", "GET", "/api/orders/" + orderId + "/details");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        OrderDetailsDTO returnValue = orderService.getOrderDetails(orderId);
        stopWatch.stop();

        log.info("Returning {} for {} {}", returnValue, "GET", "/api/orders/" + orderId + "/details");

        if (stopWatch.getTime() > 1500) {
            log.warn("Slow {} took {} ms", "getOrderDetails", stopWatch.getTime());
        }

        return ResponseEntity.ok(returnValue);
    }
    // [S3-F10] Get Order Analytics Dashboard (Report DTO)
    @GetMapping("/analytics/dashboard")
    public OrderAnalyticsDashboardDTO getOrderAnalyticsDashboard(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Received {} {}", "GET", "/api/orders/analytics/dashboard");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        OrderAnalyticsDashboardDTO returnValue = orderService.getOrderAnalyticsDashboardWrapper(startDate, endDate);
        stopWatch.stop();
        log.info("Returning {} for {} {}", returnValue, "GET", "/api/orders/analytics/dashboard");
        if (stopWatch.getTime() > 1500) {
            log.warn("Slow {} took {} ms", "getOrderAnalyticsDashboard", stopWatch.getTime());
        }
        return returnValue;
    }
    // [S3-F11] Record User-Restaurant Ordering Pattern
    @PostMapping("/{orderId}/record-interaction")
    public ResponseEntity<InteractionRecordingResponseDTO> recordInteraction(@PathVariable Long orderId) {
        log.info("Received {} {}", "POST", "/api/orders/" + orderId + "/record-interaction");
        InteractionRecordingResponseDTO returnValue = orderService.recordInteraction(orderId);
        log.info("Returning {} for {} {}", returnValue, "POST", "/api/orders/" + orderId + "/record-interaction");
        return ResponseEntity.ok(returnValue);
    }

    // [S3-F12]
    @GetMapping("/recommendations")
    public ResponseEntity<List<RestaurantRecommendationDTO>> getRestaurantRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer limit) {
        int finalLimit = (limit == null || limit <= 0) ? 5 : limit;

        log.info("Received {} {}", "GET", "/api/orders/recommendations");
        List<RestaurantRecommendationDTO> returnValue = orderService.getRestaurantRecommendations(userId, finalLimit);
        log.info("Returning {} for {} {}", returnValue, "GET", "/api/orders/recommendations");
        return ResponseEntity.ok(returnValue);
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getTotalOrderCountForUser(@PathVariable Long userId) {
        log.info("Received {} {}", "GET", "/api/orders/user/" + userId + "/count");
        ResponseEntity<Long> returnValue = ResponseEntity.ok(
                orderService.getTotalOrderCountForUser(userId)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "GET", "/api/orders/user/" + userId + "/count");
        return returnValue;
    }

    @GetMapping("/user/{userId}/active-count")
    public ResponseEntity<Long> getActiveOrderCountForUser(@PathVariable Long userId) {
        log.info("Received {} {}", "GET", "/api/orders/user/" + userId + "/active-count");
        ResponseEntity<Long> returnValue = ResponseEntity.ok(
                orderService.getActiveOrderCountForUser(userId)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "GET", "/api/orders/user/" + userId + "/active-count");
        return returnValue;
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<OrderSummaryDTO> getUserOrderSummary(@PathVariable Long userId) {
        log.info("Received {} {}", "GET", "/api/orders/user/" + userId + "/summary");
        ResponseEntity<OrderSummaryDTO> returnValue = ResponseEntity.ok(
                orderService.getUserOrderSummary(userId)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "GET", "/api/orders/user/" + userId + "/summary");
        return returnValue;
    }

    @GetMapping("/restaurant/{restaurantId}/summary")
    public ResponseEntity<RestaurantOrderSummaryDTO> getRestaurantOrderSummary(@PathVariable Long restaurantId) {
        log.info("Received {} {}", "GET", "/api/orders/restaurant/" + restaurantId + "/summary");
        ResponseEntity<RestaurantOrderSummaryDTO> returnValue = ResponseEntity.ok(
                orderService.getRestaurantOrderSummary(restaurantId)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "GET", "/api/orders/restaurant/" + restaurantId + "/summary");
        return returnValue;
    }

    // [CRUD]
    //// Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        log.info("Received {} {}", "GET", "/api/orders/" + id);
        ResponseEntity<Order> returnValue = ResponseEntity.ok(
                orderService.getOrderById(id)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "GET", "/api/orders/" + id);
        return returnValue;
    }
    //// Get all orders
    @GetMapping
    public ResponseEntity<java.util.List<Order>> getAllOrders() {
        log.info("Received {} {}", "GET", "/api/orders");
        ResponseEntity<java.util.List<Order>> returnValue = ResponseEntity.ok(
                orderService.getAllOrders()
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "GET", "/api/orders");
        return returnValue;
    }
    //// Create order
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        log.info("Received {} {}", "POST", "/api/orders");
        ResponseEntity<Order> returnValue = ResponseEntity.ok(
                orderService.createOrder(order)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "POST", "/api/orders");
        return returnValue;
    }
    //// Update order
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        log.info("Received {} {}", "PUT", "/api/orders/" + id);
        ResponseEntity<Order> returnValue = ResponseEntity.ok(
                orderService.updateOrder(id, order)
        );
        log.info("Returning {} for {} {}", returnValue.getBody(), "PUT", "/api/orders/" + id);
        return returnValue;
    }
    //// Delete order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Received {} {}", "DELETE", "/api/orders/" + id);
        orderService.deleteOrder(id);
        log.info("Returning {} for {} {}", "Deleted", "DELETE", "/api/orders/" + id);
        return ResponseEntity.noContent().build();
    }

}
