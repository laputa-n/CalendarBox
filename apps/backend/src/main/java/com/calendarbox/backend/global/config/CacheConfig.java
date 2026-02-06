package com.calendarbox.backend.global.config;

import com.calendarbox.backend.analytics.dto.response.*;
import com.calendarbox.backend.global.dto.PageResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper om) {

        ObjectMapper mapper = om.copy().findAndRegisterModules();

        RedisCacheConfiguration common = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "cache::" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                "analytics:peopleSummary", common.serializeValuesWith(pair(serializer(mapper, PeopleStatSummary.class))),
                "analytics:placeSummary",  common.serializeValuesWith(pair(serializer(mapper, PlaceStatSummary.class))),

                "analytics:peopleList", common.serializeValuesWith(pair(serializer(mapper,
                        mapper.getTypeFactory().constructParametricType(PageResponse.class, PeopleStatItem.class)))),
                "analytics:placeList", common.serializeValuesWith(pair(serializer(mapper,
                        mapper.getTypeFactory().constructParametricType(PageResponse.class, PlaceStatItem.class)))),

                "analytics:trend", common.serializeValuesWith(pair(serializer(mapper,
                        mapper.getTypeFactory().constructCollectionType(List.class, MonthlyScheduleTrend.class)))),
                "analytics:dayHour", common.serializeValuesWith(pair(serializer(mapper,
                        mapper.getTypeFactory().constructCollectionType(List.class, DayHourScheduleDistribution.class))))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(common)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    private RedisSerializationContext.SerializationPair<?> pair(RedisSerializer<?> ser) {
        return RedisSerializationContext.SerializationPair.fromSerializer(ser);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private <T> RedisSerializer<T> serializer(ObjectMapper mapper, Class<T> clazz) {
        Jackson2JsonRedisSerializer<T> ser = new Jackson2JsonRedisSerializer<>(clazz);
        ser.setObjectMapper(mapper); // (3.x에선 deprecated 경고) -> 일단 억제
        return ser;
    }

    @SuppressWarnings({"deprecation", "removal"})
    private RedisSerializer<?> serializer(ObjectMapper mapper, JavaType javaType) {
        Jackson2JsonRedisSerializer<Object> ser = new Jackson2JsonRedisSerializer<>(javaType);
        ser.setObjectMapper(mapper); // (3.x에선 deprecated 경고) -> 일단 억제
        return ser;
    }
}
