package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(Order order) {
        return orderRepository.save(order);
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
}