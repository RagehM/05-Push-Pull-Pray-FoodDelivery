package com.team05.fooddelivery.order.dto;

import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import java.util.Map;

public class OrderAnalyticsDashboardDTO {
    private long totalOrders;
    private double totalRevenue;
    private double avgOrderValue;
    private double completionRate;
    private Map<OrderStatusEnum, Long> ordersByStatus;

    public OrderAnalyticsDashboardDTO() {
        this.totalOrders = 0;
        this.totalRevenue = 0.0;
        this.avgOrderValue = 0.0;
        this.completionRate = 0.0;
        this.ordersByStatus = Map.of(
            OrderStatusEnum.DELIVERED, 0L,
            OrderStatusEnum.CANCELLED, 0L,
            OrderStatusEnum.PLACED, 0L,
            OrderStatusEnum.CONFIRMED, 0L,
            OrderStatusEnum.PREPARING, 0L
        );
    }

    public OrderAnalyticsDashboardDTO(long totalOrders, double completionRate, double totalRevenue, double avgOrderValue, Map<OrderStatusEnum, Long> ordersByStatus) {
        this.totalOrders = totalOrders;
        this.completionRate = completionRate;
        this.totalRevenue = totalRevenue;
        this.avgOrderValue = avgOrderValue;
        this.ordersByStatus = ordersByStatus;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public Map<OrderStatusEnum, Long> getOrdersByStatus() {
        return ordersByStatus;
    }

    public void setOrdersByStatus(Map<OrderStatusEnum, Long> ordersByStatus) {
        this.ordersByStatus = ordersByStatus;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(double avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalOrders;
        private double completionRate;
        private double totalRevenue;
        private double avgOrderValue;
        private Map<OrderStatusEnum, Long> ordersByStatus;

        public Builder totalOrders(long totalOrders) {
            this.totalOrders = totalOrders;
            return this;
        }

        public Builder completionRate(double completionRate) {
            this.completionRate = completionRate;
            return this;
        }

        public Builder ordersByStatus(Map<OrderStatusEnum, Long> ordersByStatus) {
            this.ordersByStatus = ordersByStatus;
            return this;
        }

        public Builder totalRevenue(double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder avgOrderValue(double avgOrderValue) {
            this.avgOrderValue = avgOrderValue;
            return this;
        }

        public OrderAnalyticsDashboardDTO build() {
            return new OrderAnalyticsDashboardDTO(totalOrders, completionRate, totalRevenue, avgOrderValue, ordersByStatus);
        }
    }


}