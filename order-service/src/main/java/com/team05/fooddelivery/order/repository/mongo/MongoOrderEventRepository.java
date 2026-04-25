package com.team05.fooddelivery.order.repository.mongo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.order.model.mongo.OrderEvent;

@Repository
public interface MongoOrderEventRepository extends MongoRepository<OrderEvent, String> {
    List<OrderEvent> findByAction(String action);
    List<OrderEvent> findByTimestampBetween(LocalDateTime min, LocalDateTime max);
    List<OrderEvent> findByOrderId(Long orderId);
}
