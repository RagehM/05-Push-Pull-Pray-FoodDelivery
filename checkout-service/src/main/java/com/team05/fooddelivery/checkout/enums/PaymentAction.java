package com.team05.fooddelivery.checkout.enums;

public enum PaymentAction {
    CREATED,
    COMPLETED,
    FAILED,
    REFUNDED,
    REFUND_DENIED,
    ANALYTICS_VIEWED,
    PAYMENT_CREATED,
    PAYMENT_UPDATED,
    PAYMENT_DELETED,
    OFFER_APPLIED,
    RETRY_ATTEMPTED;

    public static boolean isValidAction(String action) {
        for (PaymentAction paymentAction : PaymentAction.values()) {
            if (paymentAction.name().equals(action)) {
                return true;
            }
        }
        return false;
    }
}
