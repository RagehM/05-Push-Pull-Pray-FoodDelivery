package com.team05.fooddelivery.checkout.dto;

public record RevenueReportDTO(
        Double totalRevenue,
        Integer totalTransactions,
        Double averagePayment,
        Double refundedAmount,
        Integer refundCount
) {}
