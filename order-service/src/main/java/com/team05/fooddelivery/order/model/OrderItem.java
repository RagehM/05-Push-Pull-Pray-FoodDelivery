package com.team05.fooddelivery.order.model;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Integer lineNumber;
    @Column(nullable = false)
    private Long menuItemId; // TODO: FK reference to MenuItem ManyToOne, Handles in OrderRepository
    @Column(nullable = false)
    private String itemName;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private Double unitPrice;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemStatusEnum status;
    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @PrePersist
    void setDefaults() {
        if (status == null) status = OrderItemStatusEnum.PENDING;
        if (lineNumber == null && order != null) {
            lineNumber = order.getOrderItems().size() + 1;
        }
        if (metadata == null) metadata = Map.of();
    }
    
    public OrderItem() {
        status = OrderItemStatusEnum.PENDING;
    }
    public OrderItem(Integer lineNumber, Long menuItemId, String itemName, Integer quantity, Double unitPrice,
            OrderItemStatusEnum status, Map<String, Object> metadata, Order order) {
        this.lineNumber = lineNumber;
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = status;
        this.metadata = metadata;
        this.order = order;
    }
    
    public OrderItem(Integer lineNumber, Long menuItemId, String itemName, Integer quantity, Double unitPrice,
            Map<String, Object> metadata, Order order) {
        this(lineNumber, menuItemId, itemName, quantity, unitPrice, OrderItemStatusEnum.PENDING, metadata, order);
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Integer getLineNumber() {
        return lineNumber;
    }
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
    public Long getMenuItemId() {
        return menuItemId;
    }
    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Double getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }
    public OrderItemStatusEnum getStatus() {
        return status;
    }
    public void setStatus(OrderItemStatusEnum status) {
        this.status = status;
    }
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    public Order getOrder() {
        return order;
    }
    public void setOrder(Order order) {
        this.order = order;
    }

    
}
