package com.team05.fooddelivery.order.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}