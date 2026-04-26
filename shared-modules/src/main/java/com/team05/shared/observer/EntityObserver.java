package com.team05.shared.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}