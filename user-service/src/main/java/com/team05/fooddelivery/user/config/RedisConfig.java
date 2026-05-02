package com.team05.fooddelivery.user.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

        cacheConfigs.put("user-service::user", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        cacheConfigs.put("user-service::S1-F1", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("user-service::S1-F3", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("user-service::S1-F5", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("user-service::S1-F6", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("user-service::S1-F8", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("user-service::S1-F9", defaultConfig.entryTtl(Duration.ofMinutes(10)));

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