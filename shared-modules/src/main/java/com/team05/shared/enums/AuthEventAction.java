package com.team05.shared.enums;

public enum AuthEventAction {
    REGISTERED,
    LOGGED_IN,
    ROLE_CHANGED,
    USER_UPDATED,
    USER_DEACTIVATED,
    DEFAULT_ADDRESS_SET,
    USER_CREATED,
    USER_DELETED;

    public static boolean isValidAction(String action) {
        for (AuthEventAction eventAction : AuthEventAction.values()) {
            if (eventAction.name().equals(action)) {
                return true;
            }
        }
        return false;
    }
}