//s2-f11
package com.team05.fooddelivery.restaurant.service;

import com.team05.fooddelivery.restaurant.model.Restaurant;
import com.team05.fooddelivery.restaurant.model.elasticsearch.RestaurantSearchDocument;
import com.team05.fooddelivery.restaurant.repository.elasticsearch.RestaurantSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RestaurantElasticsearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantElasticsearchIndexService.class);

    private final RestaurantSearchRepository searchRepository;

    public RestaurantElasticsearchIndexService(RestaurantSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public void upsertFromRestaurant(Restaurant restaurant) {
        if (restaurant == null || restaurant.getId() == null) {
            return;
        }
        try {
            searchRepository.save(toDocument(restaurant));
        } catch (Exception e) {
            log.warn("Elasticsearch upsert skipped for restaurantId={}: {}", restaurant.getId(), e.getMessage());
        }
    }

    public void deleteByRestaurantId(Long restaurantId) {
        if (restaurantId == null) {
            return;
        }
        try {
            searchRepository.deleteById(restaurantId);
        } catch (Exception e) {
            log.warn("Elasticsearch delete skipped for restaurantId={}: {}", restaurantId, e.getMessage());
        }
    }

    private static RestaurantSearchDocument toDocument(Restaurant restaurant) {
        RestaurantSearchDocument doc = new RestaurantSearchDocument();
        doc.setId(restaurant.getId());
        doc.setName(restaurant.getName());
        doc.setCuisineType(restaurant.getCuisineType() != null ? restaurant.getCuisineType().name() : null);
        doc.setDescription(resolveDescription(restaurant.getDetails()));
        doc.setRating(restaurant.getRating());
        doc.setStatus(restaurant.getStatus() != null ? restaurant.getStatus().name() : null);
        return doc;
    }

    private static String resolveDescription(Map<String, Object> details) {
        if (details == null) {
            return "";
        }
        Object raw = details.get("description");
        if (raw == null) {
            return "";
        }
        return raw.toString();
    }
}