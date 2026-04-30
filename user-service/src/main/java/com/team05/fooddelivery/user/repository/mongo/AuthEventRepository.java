package com.team05.fooddelivery.user.repository.mongo;

import com.team05.fooddelivery.user.model.mongo.AuthEvent;
import com.team05.shared.repository.mongo.MongoEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthEventRepository extends MongoEventRepository<AuthEvent, String> {
}