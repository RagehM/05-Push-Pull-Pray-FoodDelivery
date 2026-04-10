package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import com.team05.fooddelivery.order.dto.OrderCostEstimateDTO;
import com.team05.fooddelivery.order.dto.OrderEstimateRequest;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.model.OrderItem;
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
        boolean userExists = orderRepository.existsByUserId(order.getUserId());
        if (!userExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }
        if(order.getRestaurantId() != null){
            boolean restaurantExists = orderRepository.existsByRestaurantId(order.getRestaurantId());
            if (!restaurantExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant not found");
        }
        }


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

        if (updatedOrder.getRestaurantId() != null){
            boolean restaurantExists = orderRepository.existsByRestaurantId(updatedOrder.getRestaurantId());
            if (!restaurantExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New restaurant you're trying to set does not exist");
            }

            existingOrder.setRestaurantId(updatedOrder.getRestaurantId());
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
    @Transactional
    public Order deliverOrder(Long id) {
        Order foundOrder = orderRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (foundOrder.getStatus() != OrderStatusEnum.PREPARING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order can only be delivered if it is in preparation");

        foundOrder.setStatus(OrderStatusEnum.DELIVERED);
        foundOrder.setDeliveredAt(LocalDateTime.now());

        if (foundOrder.getTotalAmount() == null) {
            List<OrderItem> orderItems = foundOrder.getOrderItems();
            double total = orderItems.stream().mapToDouble(i-> i.getQuantity() *i.getUnitPrice()).sum();
            foundOrder.setTotalAmount(total);
        }
        // Create payment record with status PENDING. Save order. Return the order after the update.
        orderRepository.createPaymentWithPendingStatus(foundOrder.getId(), foundOrder.getUserId(), foundOrder.getTotalAmount());
        return orderRepository.save(foundOrder);


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
    ////Get Order Cost Estimate Service
    public OrderCostEstimateDTO estimateOrderCost(OrderEstimateRequest request) {
        if(request == null || request.restaurantId() == null || request.itemCount() == null || request.deliveryDistance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order estimate request");
        }
        if (request.itemCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemCount must be greater than 0");
        }
        if (request.deliveryDistance() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deliveryDistance must be > 0");
        }
        boolean restaurantExists = orderRepository.existsByRestaurantId(request.restaurantId());
        if (!restaurantExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }
        Double avgMenuPrice = orderRepository.findAverageMenuItemPriceByRestaurantId(request.restaurantId());
        if (avgMenuPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant has no menu items right now");
        }
        double foodCost = avgMenuPrice * request.itemCount();
        double deliveryFee = 10.0 * request.deliveryDistance();
        double serviceFee = foodCost * 0.05;
        long activeOrders = orderRepository.countActiveOrdersByRestaurantId(request.restaurantId());
        double surgeMultiplier = activeOrders > 10 ? (activeOrders > 20 ? 1.5: 1.2) : 1.0;
        double total = (foodCost+deliveryFee + serviceFee) * surgeMultiplier;
        return new OrderCostEstimateDTO(
                foodCost,
                deliveryFee,
                serviceFee,
                total,
                surgeMultiplier
        );

    }

}