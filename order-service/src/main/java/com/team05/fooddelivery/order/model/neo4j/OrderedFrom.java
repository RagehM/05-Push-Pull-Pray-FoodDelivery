package com.team05.fooddelivery.order.model.neo4j;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class OrderedFrom {

    @RelationshipId
    private Long id;

    private int orderCount;
    private LocalDateTime lastOrderDate;

    // Track recorded order IDs for idempotency
    private Set<Long> recordedOrderIds = new HashSet<>();

    @TargetNode
    private RestaurantNode restaurant;

    public OrderedFrom(RestaurantNode restaurant) {
        this.restaurant = restaurant;
        this.recordedOrderIds = new HashSet<>();
    }

    public Long getId() { return id; }

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) {this.lastOrderDate = lastOrderDate; }

    public Set<Long> getRecordedOrderIds() {
        return recordedOrderIds;
    }
    public void setRecordedOrderIds(Set<Long> recordedOrderIds) {
        this.recordedOrderIds = recordedOrderIds;
    }

    public RestaurantNode getRestaurant() { return restaurant; }
    public void setRestaurant(RestaurantNode restaurant) { this.restaurant = restaurant; }

    // check if order was already recorded (idempotency)
    public boolean isOrderRecorded(Long orderId) {
        return recordedOrderIds.contains(orderId);
    }

    // mark order as recorded
    public void recordOrderId(Long orderId) {
        recordedOrderIds.add(orderId);
    }


}