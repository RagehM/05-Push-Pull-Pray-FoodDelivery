package com.team05.shared.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.team05.shared.model.mongo.MongoEvent;

public interface MongoEventRepository<T extends MongoEvent, ID> extends MongoRepository<T, ID> {
}