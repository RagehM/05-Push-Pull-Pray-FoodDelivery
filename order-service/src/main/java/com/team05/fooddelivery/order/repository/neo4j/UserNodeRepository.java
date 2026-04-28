package com.team05.fooddelivery.order.repository.neo4j;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.team05.fooddelivery.order.model.neo4j.UserNode;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
    Optional<UserNode> findByName(String name);
}
