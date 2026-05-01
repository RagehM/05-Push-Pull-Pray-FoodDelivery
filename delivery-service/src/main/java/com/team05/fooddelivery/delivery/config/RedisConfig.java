package com.team05.fooddelivery.delivery.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@EnableCaching
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {

        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper()));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // CRUD GET-by-ID — 15 min
        cacheConfigs.put("delivery-service::delivery",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // S4-F1: Get Latest Delivery for an Order — 5 min
        cacheConfigs.put("delivery-service::S4-F1",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // S4-F3: Find Nearby Deliveries — 10 min
        cacheConfigs.put("delivery-service::S4-F3",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // S4-F5: Filter Deliveries by Metadata — 5 min
        cacheConfigs.put("delivery-service::S4-F5",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // S4-F6: Order Delivery History in Date Range — 10 min
        cacheConfigs.put("delivery-service::S4-F6",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // S4-F8: Delivery Performance Summary — 15 min
        cacheConfigs.put("delivery-service::S4-F8",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // S4-F9: Find Delayed Deliveries — 10 min
        cacheConfigs.put("delivery-service::S4-F9",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // S4-F10: Delivery Analytics Dashboard (M2) — 10 min
        cacheConfigs.put("delivery-service::S4-F10",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // S4-F12: Delivery Tracking Timeline (M2) — 5 min
        cacheConfigs.put("delivery-service::S4-F12",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(
                new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        return template;
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
