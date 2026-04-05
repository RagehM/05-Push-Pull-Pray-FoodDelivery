package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.model.Order;
import com.team05.fooddelivery.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<Order> searchOrdersByMetadata(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata key must not be empty");
        }

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadata value must not be empty");
        }

        return orderRepository.findByMetadataKeyValue(key.trim(), value.trim());
    }
}