package com.team05.fooddelivery.order.adapter;

import com.team05.fooddelivery.order.dto.OrderAnalyticsDTO;

public class orderArrayToOrderAnalyticsDTOAdapter {
    public static OrderAnalyticsDTO adapt(Object[] data) {
        if (data == null || data.length < 6) {
            throw new IllegalArgumentException("Data array must have at least 6 elements");
        }
        return OrderAnalyticsDTO.builder()
                .totalOrders((Long) data[0])
                .deliveredOrders((Long) data[1])
                .cancelledOrders((Long) data[2])
                .totalRevenue((Double) data[3])
                .averageOrderAmount((Double) data[4])
                .deliveryRate((Double) data[5])
                .build();
    }
}
