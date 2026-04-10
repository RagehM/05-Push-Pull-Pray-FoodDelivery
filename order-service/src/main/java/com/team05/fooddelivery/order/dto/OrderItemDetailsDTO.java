package com.team05.fooddelivery.order.dto;

import com.team05.fooddelivery.order.enums.OrderItemStatusEnum;
import java.util.Map;

public class OrderItemDetailsDTO {
    private Long id;
    private Integer lineNumber;
    private String itemName;
    private Integer quantity;
    private Double unitPrice;
    private OrderItemStatusEnum status;
    private Map<String, Object> metadata;

    public OrderItemDetailsDTO() {
    }

    public OrderItemDetailsDTO(Long id, Integer lineNumber, String itemName,
                               Integer quantity, Double unitPrice,
                               OrderItemStatusEnum status, Map<String, Object> metadata) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.status = status;
        this.metadata = metadata;
    }

    public Long getId() {
        return id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public OrderItemStatusEnum getStatus() {
        return status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setStatus(OrderItemStatusEnum status) {
        this.status = status;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}