package com.team05.fooddelivery.checkout.dto;

import com.team05.fooddelivery.checkout.enums.PaymentMethod;

/**
 * Response DTO for [S5-F11] Get Payment Method Breakdown.
 *
 * Each row represents an aggregation over the payment_audit_trail
 * MongoDB collection for a single PaymentMethod within the requested
 * date range:
 *   - successCount: number of COMPLETED audit events
 *   - failureCount: number of FAILED audit events
 *   - successRate : successCount / (successCount + failureCount), 0 if no events
 *   - totalAmount : sum of {@code amount} across COMPLETED events only
 */
public record PaymentMethodDTO(
        PaymentMethod method,
        long successCount,
        long failureCount,
        double successRate,
        double totalAmount
) {
}
