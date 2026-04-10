package com.team05.fooddelivery.order.dto;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import java.util.List;
import java.util.Map;

public class OrderDetailsDTO {
    private Long orderId;
    private Long userId;
    private Long restaurantId;
    private OrderStatusEnum status;
    private Double totalAmount;
    private Map<String, Object> metadata;
    private List<OrderItemDetailsDTO> items;
    private Integer totalItems;
    private Integer preparedItems;

    public OrderDetailsDTO() {
    }

    public OrderDetailsDTO(Long orderId, Long userId, Long restaurantId,
                           OrderStatusEnum status, Double totalAmount,
                           Map<String, Object> metadata,
                           List<OrderItemDetailsDTO> items,
                           Integer totalItems, Integer preparedItems) {
        this.orderId = orderId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.metadata = metadata;
        this.items = items;
        this.totalItems = totalItems;
        this.preparedItems = preparedItems;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public OrderStatusEnum getStatus() {
        return status;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<OrderItemDetailsDTO> getItems() {
        return items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public Integer getPreparedItems() {
        return preparedItems;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void setItems(List<OrderItemDetailsDTO> items) {
        this.items = items;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public void setPreparedItems(Integer preparedItems) {
        this.preparedItems = preparedItems;
    }
}