package com.team05.shared.enums;

import java.util.Arrays;
import java.util.Locale;

/**
 * Delivery action constants used for validation of delivery events.
 * Keep values in UPPER_SNAKE_CASE to match project conventions.
 */
public enum DeliveryAction {
    TRACKING_RECORDED,
    ANALYTICS_VIEWED,
    DELIVERY_CREATED,
    DELIVERY_UPDATED,
    DELIVERY_DELETED,
    BATCH_STATUS_UPDATED,
    OLD_DATA_PURGED;

    /**
     * Case-insensitive validation helper. Accepts both exact enum names and common variations.
     */
    public static boolean isValidAction(String action) {
        if (action == null) return false;
        for (DeliveryAction a : DeliveryAction.values()) {
            if (a.name().equals(action)) {
                return true;
            }
        }
        return false;
    }
}


