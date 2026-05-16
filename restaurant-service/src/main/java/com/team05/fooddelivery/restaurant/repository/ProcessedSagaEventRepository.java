package com.team05.fooddelivery.restaurant.repository;

import com.team05.fooddelivery.restaurant.model.ProcessedSagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedSagaEventRepository extends JpaRepository<ProcessedSagaEvent, String> {
}
