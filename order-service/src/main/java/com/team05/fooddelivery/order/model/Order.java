package com.team05.fooddelivery.order.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId; //TODO: FK reference to User ManyToOne, Handles in OrderRepository
    @Column(nullable = true)
    private Long restaurantId; //TODO: FK reference to Restaurant ManyToOne, Handles in OrderRepository
    @Column(nullable = false)
    private String deliveryAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatusEnum status;
    @Column(nullable = true)
    private Double totalAmount;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;
    @Column(nullable = false)
    private LocalDateTime orderDate;
    @Column(nullable = true)
    private LocalDateTime deliveredAt;
    @JsonIgnore
    @OneToMany(mappedBy = "id", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<OrderItem>();
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public Long getRestaurantId() {
        return restaurantId;
    }
    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    public OrderStatusEnum getStatus() {
        return status;
    }
    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }
    public Double getTotalAmount() {
        return totalAmount;
    }
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

}