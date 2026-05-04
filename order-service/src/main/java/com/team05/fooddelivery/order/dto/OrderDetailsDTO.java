package com.team05.fooddelivery.order.dto;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OrderDetailsDTO {
    private final Long orderId;
    private final Long userId;
    private final Long restaurantId;
    private final OrderStatusEnum status;
    private final Double totalAmount;
    private final Map<String, Object> metadata;
    private final List<OrderItemDetailsDTO> items;
    private final Integer totalItems;
    private final Integer preparedItems;

    private OrderDetailsDTO(Builder builder) {
        this.orderId = builder.orderId;
        this.userId = builder.userId;
        this.restaurantId = builder.restaurantId;
        this.status = builder.status;
        this.totalAmount = builder.totalAmount;
        this.metadata = builder.metadata;
        this.items = builder.items != null
                ? Collections.unmodifiableList(builder.items)
                : Collections.emptyList();
        if (builder.totalItems == null) {
            this.totalItems = builder.items != null ? builder.items.size() : 0;
        } else {
            this.totalItems = builder.totalItems;
        }
        this.preparedItems = builder.preparedItems != null ? builder.preparedItems : 0;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getRestaurantId() { return restaurantId; }
    public OrderStatusEnum getStatus() { return status; }
    public Double getTotalAmount() { return totalAmount; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<OrderItemDetailsDTO> getItems() { return items; }
    public Integer getTotalItems() { return totalItems; }
    public Integer getPreparedItems() { return preparedItems; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long orderId;
        private Long userId;
        private Long restaurantId;
        private OrderStatusEnum status;
        private Double totalAmount;
        private Map<String, Object> metadata;
        private List<OrderItemDetailsDTO> items;
        private Integer preparedItems;
        private Integer totalItems;

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder restaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
            return this;
        }

        public Builder status(OrderStatusEnum status) {
            this.status = status;
            return this;
        }

        public Builder totalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder items(List<OrderItemDetailsDTO> items) {
            this.items = items;
            return this;
        }

        public Builder preparedItems(Integer preparedItems) {
            this.preparedItems = preparedItems;
            return this;
        }

        public Builder totalItems(Integer totalItems) {
            this.totalItems = totalItems;
            return this;
        }

        public OrderDetailsDTO build() {
            if (orderId == null)
                throw new IllegalStateException("orderId is required");
            if (userId == null)
                throw new IllegalStateException("userId is required");
            if (status == null)
                throw new IllegalStateException("status is required");
            if (totalAmount == null || totalAmount < 0)
                throw new IllegalStateException("totalAmount must be non-negative");

            return new OrderDetailsDTO(this);
        }
    }
}