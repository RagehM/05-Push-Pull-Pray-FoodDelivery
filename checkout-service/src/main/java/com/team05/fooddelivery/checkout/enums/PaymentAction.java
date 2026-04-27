package com.team05.fooddelivery.checkout.enums;

public enum PaymentAction {
    CREATED,
    COMPLETED,
    FAILED,
    REFUNDED,
    REFUND_DENIED,
    ANALYTICS_VIEWED;

    public static boolean isValidAction(String action) {
        for (PaymentAction paymentAction : PaymentAction.values()) {
            if (paymentAction.name().equals(action)) {
                return true;
            }
        }
        return false;
    }
}
