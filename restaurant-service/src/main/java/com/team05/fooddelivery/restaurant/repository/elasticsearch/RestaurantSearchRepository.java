//s2-f11
package com.team05.fooddelivery.restaurant.repository.elasticsearch;

import com.team05.fooddelivery.restaurant.model.elasticsearch.RestaurantSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RestaurantSearchRepository extends ElasticsearchRepository<RestaurantSearchDocument, Long> {
}