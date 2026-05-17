package com.team05.fooddelivery.restaurant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_saga_events")
public class ProcessedSagaEvent {

    @Id
    @Column(length = 128)
    private String eventKey;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column
    private Double contextAmount;

    protected ProcessedSagaEvent() {
    }

    public ProcessedSagaEvent(String eventKey, LocalDateTime processedAt) {
        this(eventKey, processedAt, null);
    }

    public ProcessedSagaEvent(String eventKey, LocalDateTime processedAt, Double contextAmount) {
        this.eventKey = eventKey;
        this.processedAt = processedAt;
        this.contextAmount = contextAmount;
    }

    public String getEventKey() {
        return eventKey;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public Double getContextAmount() {
        return contextAmount;
    }
}
