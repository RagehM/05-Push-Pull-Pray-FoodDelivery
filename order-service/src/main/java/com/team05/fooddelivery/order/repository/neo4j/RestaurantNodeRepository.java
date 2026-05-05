package com.team05.fooddelivery.order.repository.neo4j;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.order.model.neo4j.RestaurantNode;

@Repository
public interface RestaurantNodeRepository extends Neo4jRepository<RestaurantNode, Long> {

    // [S3-F11] Find restaurant by PostgreSQL restaurant ID
    @Query("""
        MATCH (r:Restaurant) WHERE r.restaurantId = $restaurantId
        RETURN r
        """)
    Optional<RestaurantNode> findByRestaurantId(@Param("restaurantId") Long restaurantId);
}