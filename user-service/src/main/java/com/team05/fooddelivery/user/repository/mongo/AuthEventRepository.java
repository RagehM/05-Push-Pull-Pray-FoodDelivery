package com.team05.fooddelivery.user.repository.mongo;

import com.team05.shared.model.mongo.AuthEvent;
import com.team05.shared.repository.mongo.MongoEventRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthEventRepository extends MongoEventRepository<AuthEvent, String> {

    @Aggregation(pipeline = {
            "{ $match: { 'userId': ?0 } }",
            "{ $sort: { timestamp: -1 } }",
            "{ $limit :  ?2 }"
    })
    List<AuthEvent> findByUserIdAndSortDescTimestamp(long userId, int page, int size);
}