package com.team05.fooddelivery.order.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public class OrderEventDTO {
    private final String id;
    private final Long orderId;
    private final String action;
    private final Map<String, Object> payload;
    private final LocalDateTime timestamp;

    private OrderEventDTO(Builder builder) {
        this.id = builder.id;
        this.orderId = builder.orderId;
        this.action = builder.action;
        this.payload = builder.payload;
        this.timestamp = builder.timestamp;
    }

    @JsonCreator
    private OrderEventDTO(
            String id,
            Long orderId,
            String action,
            Map<String, Object> payload,
            LocalDateTime timestamp
    ) {
        this.id = id;
        this.orderId = orderId;
        this.action = action;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public static class Builder {
        private String id;
        private Long orderId;
        private String action;
        private Map<String, Object> payload;
        private LocalDateTime timestamp;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public OrderEventDTO build() {
            return new OrderEventDTO(this);
        }
    }
}
