package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;

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
}