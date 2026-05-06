package com.team05.fooddelivery.order.repository.neo4j;

import java.util.List;
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
        MATCH (u:User {userId: $userId})
        OPTIONAL MATCH (u)-[rel:ORDERED_FROM]->(r:Restaurant)
        RETURN u, collect(rel), collect(r)
    """)
    Optional<UserNode> findByUserIdWithRelationships(@Param("userId") Long userId);

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

    // [S3-F12]

    @Query(value = """
        MATCH (u:User {userId: $userId})-[:ORDERED_FROM]->(common:Restaurant)<-[:ORDERED_FROM]-(similar:User)
        MATCH (similar)-[:ORDERED_FROM]->(rec:Restaurant)
        WHERE NOT (u)-[:ORDERED_FROM]->(rec)
        RETURN rec.restaurantId AS restaurantId, count(DISTINCT similar) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<RestaurantRecommendationRow> findRecommendations(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    interface RestaurantRecommendationRow {
        Long getRestaurantId();
        Long getScore();
    }
}