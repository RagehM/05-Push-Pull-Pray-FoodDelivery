package com.team05.fooddelivery.user.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = TopCustomerDTO.Builder.class)
public class TopCustomerDTO {
    private Long userId;
    private String name;
    private Double totalSpent;
    private Integer orderCount;

    private TopCustomerDTO(Builder builder) {
        this.userId = builder.userId;
        this.name = builder.name;
        this.totalSpent = builder.totalSpent;
        this.orderCount = builder.orderCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Long userId;
        private String name;
        private Double totalSpent;
        private Integer orderCount;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalSpent(Double totalSpent) {
            this.totalSpent = totalSpent;
            return this;
        }

        public Builder orderCount(Integer orderCount) {
            this.orderCount = orderCount;
            return this;
        }

        public TopCustomerDTO build() {
            return new TopCustomerDTO(this);
        }
    }
}