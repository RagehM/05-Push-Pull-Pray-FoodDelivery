package com.team05.fooddelivery.delivery.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public class BatchDeliveryRequestDTO {

    private Long orderId;

    @JsonAlias({"updates", "deliveryUpdates", "items", "deliveryItems"})
    private List<DeliveryItemDTO> deliveries;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public List<DeliveryItemDTO> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<DeliveryItemDTO> deliveries) {
        this.deliveries = deliveries;
    }
}
