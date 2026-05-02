package com.team05.fooddelivery.order.adapter;

import org.neo4j.driver.Record;
import com.team05.fooddelivery.order.dto.RestaurantRecommendationDTO;

public class Neo4jRecordAdapter {

    public RestaurantRecommendationDTO adapt(Record record) {
        return new RestaurantRecommendationDTO.Builder()
                .id(record.get("id").asLong())
                .name(record.get("name").asString())
                .cuisineType(record.get("cuisineType").asString())
                .build();
    }
}