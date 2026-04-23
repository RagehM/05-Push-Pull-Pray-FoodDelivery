package com.team05.fooddelivery.restaurant.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // TTLs from spec Section 4.4.1 and 4.4.2
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // F1 = 5 min (search)
        cacheConfigs.put("restaurant-service::S2-F1",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // F3 = 10 min (DTO)
        cacheConfigs.put("restaurant-service::S2-F3",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // F5 = 5 min (JSONB query)
        cacheConfigs.put("restaurant-service::S2-F5",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // F6 = 10 min (report)
        cacheConfigs.put("restaurant-service::S2-F6",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // F9 = 10 min (combined)
        cacheConfigs.put("restaurant-service::S2-F9",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // CRUD GET-by-ID = 15 min (Section 4.4.2)
        cacheConfigs.put("restaurant-service::restaurant",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("restaurant-service::menu-item",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}