package com.team05.fooddelivery.order.repository.neo4j;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.order.model.neo4j.UserNode;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
    Optional<UserNode> findByName(String name);

    // [S3-F11] Find user by PostgreSQL user ID
    @Query(value = """
        MATCH (u:User) WHERE u.userId = $userId
        RETURN u
        """)
    Optional<UserNode> findByUserId(@Param("userId") Long userId);

    // [S3-F11] Check idempotency: does the relationship already have this orderId recorded?
    @Query(value = """
        MATCH (u:User {userId: $userId})-[rel:ORDERED_FROM]->(r:Restaurant {restaurantId: $restaurantId})
        WHERE $orderId IN rel.recordedOrderIds
        RETURN COUNT(rel) > 0 AS isRecorded
        """)
    boolean isOrderRecordedInRelationship(
            @Param("userId") Long userId,
            @Param("restaurantId") Long restaurantId,
            @Param("orderId") Long orderId
    );
}
