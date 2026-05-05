package com.team05.fooddelivery.restaurant.adapter;

import com.team05.fooddelivery.restaurant.model.elasticsearch.RestaurantSearchDocument;
import org.springframework.data.elasticsearch.core.SearchHit;

// Section 3.8 — Adapter Pattern
// Converts a raw Elasticsearch SearchHit into the RestaurantSearchDocument DTO
// Used by S2-F10 (Full-Text Restaurant Search)
public class ElasticsearchHitAdapter {

    public RestaurantSearchDocument adapt(SearchHit<RestaurantSearchDocument> hit) {
        if (hit == null) {
            return null;
        }
        return hit.getContent();
    }
}