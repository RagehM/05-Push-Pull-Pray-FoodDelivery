package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public List<Order> searchOrders(OrderStatusEnum status, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate.plusDays(1).atStartOfDay();

        return orderRepository.searchByStatusAndDateRange(
                status,
                startDateTime,
                endDateTimeExclusive
        );
    }
    @Transactional
    public void cancelOrder(Long orderId) {
        //Get order through JPA default method findById, if order not found, throw HTTP 404 Not Found
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        //Check Status, if status != PLACED or CONFIRMED, throw HTTP 400 Bad Request
        if (!(order.getStatus() == OrderStatusEnum.PLACED || order.getStatus() == OrderStatusEnum.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order cannot be cancelled as it is already: " + order.getStatus());
        }
        //Update status to CANCELLED
        order.setStatus(OrderStatusEnum.CANCELLED);
        orderRepository.save(order);
        //Update status of all order items to cancelled through repository
        // // // try {
        // // //     orderRepository.updateOrderItemsStatusByOrderId(orderId, OrderItemStatusEnum.CANCELLED);
        // // // } catch (Exception e) {
        // // //     System.err.println("Failed to cancel order items for orderId " + orderId + ": " + e.getMessage());
        // // //     throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to cancel order items: " + e.getMessage());
        // // // }
        //Call SQL query from within orderRepository to update deliveryStatus to CANCELLED
        try {
            orderRepository.cancelDeliveryByOrderId(orderId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Failed to cancel delivery: " + e.getMessage());
        }
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
    
}