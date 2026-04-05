package com.team05.fooddelivery.order.service;

import com.team05.fooddelivery.order.model.Order;

import java.util.List;

public interface OrderService {
    List<Order> searchOrdersByMetadata(String key, String value);
}