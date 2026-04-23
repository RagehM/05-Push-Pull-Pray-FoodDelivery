package com.team05.fooddelivery.restaurant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CacheInvalidationUtil {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationUtil.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInvalidationUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Deletes a specific cache key
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Evicted cache key: {}", key);
        } catch (Exception e) {
            log.warn("Failed to evict cache key {}: {}", key, e.getMessage());
        }
    }

    // Deletes all keys matching a pattern (wildcard)
    // Spec ref: Section 4.4.6
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Failed to evict keys for pattern {}: {}", pattern, e.getMessage());
        }
    }
}