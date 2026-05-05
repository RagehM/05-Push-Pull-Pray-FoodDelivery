package com.team05.fooddelivery.order.adapter;

import com.team05.fooddelivery.order.dto.OrderAnalyticsDashboardDTO;
import com.team05.fooddelivery.order.enums.OrderStatusEnum;
import java.util.Map;
import java.util.HashMap;

public class ObjectArrayToOrderAnalyticsDashboardDTOAdapter {
    public static OrderAnalyticsDashboardDTO adapt(Object[] objects) {
        if (objects == null || objects.length != 8) {
            throw new IllegalArgumentException("Invalid input data for OrderAnalyticsDashboardDTO");
        }

        long totalOrders = ((Number) objects[0]).longValue();
        double totalRevenue = ((Number) objects[1]).doubleValue();
        double completionRate = ((Number) objects[2]).doubleValue();
        long deliveredOrders = ((Number) objects[3]).longValue();
        long cancelledOrders = ((Number) objects[4]).longValue();
        long placedOrders = ((Number) objects[5]).longValue();
        long confirmedOrders = ((Number) objects[6]).longValue();
        long preparingOrders = ((Number) objects[7]).longValue();

        double averageOrderValue = deliveredOrders > 0 ? totalRevenue / deliveredOrders : 0.0;

        System.out.println("Parsed analytics data - Total Orders: " + totalOrders + ", Completion Rate: " + completionRate);
        System.out.println("Orders by Status - Delivered: " + deliveredOrders + ", Placed: " + placedOrders + ", Cancelled: " + cancelledOrders + ", Confirmed: "
            + confirmedOrders + ", Preparing: " + preparingOrders);

        Map<OrderStatusEnum, Long> ordersByStatus = new HashMap<>();
        if (deliveredOrders > 0) {
            ordersByStatus.put(OrderStatusEnum.DELIVERED, deliveredOrders);
        }
        if (cancelledOrders > 0) {
            ordersByStatus.put(OrderStatusEnum.CANCELLED, cancelledOrders);
        }
        if (placedOrders > 0) {
            ordersByStatus.put(OrderStatusEnum.PLACED, placedOrders);
        }
        if (confirmedOrders > 0) {  
            ordersByStatus.put(OrderStatusEnum.CONFIRMED, confirmedOrders);
        }
        if (preparingOrders > 0) {
            ordersByStatus.put(OrderStatusEnum.PREPARING, preparingOrders);
        }
        // ordersByStatus.put(OrderStatusEnum.DELIVERED, deliveredOrders);
        // ordersByStatus.put(OrderStatusEnum.CANCELLED, cancelledOrders);
        // ordersByStatus.put(OrderStatusEnum.PLACED, placedOrders);
        // ordersByStatus.put(OrderStatusEnum.CONFIRMED, confirmedOrders);
        // ordersByStatus.put(OrderStatusEnum.PREPARING, preparingOrders);

        return OrderAnalyticsDashboardDTO.builder()
            .totalOrders(totalOrders)
            .completionRate(completionRate)
            .totalRevenue(totalRevenue)
            .avgOrderValue(averageOrderValue)
            .ordersByStatus(ordersByStatus)
            .build();
    }
}
