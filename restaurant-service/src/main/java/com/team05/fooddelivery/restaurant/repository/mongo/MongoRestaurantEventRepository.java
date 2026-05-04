package com.team05.fooddelivery.restaurant.repository.mongo;

import com.team05.shared.model.mongo.RestaurantEvent;
import com.team05.shared.repository.mongo.MongoEventRepository;

// Section 3.3 — Observer Pattern
// Repository that saves RestaurantEvent documents to MongoDB "restaurant_events" collection
public interface MongoRestaurantEventRepository extends MongoEventRepository<RestaurantEvent, String> {
}