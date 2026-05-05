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