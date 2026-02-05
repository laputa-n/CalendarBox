package com.calendarbox.backend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheErrorHandlerConfig {

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.error("[CACHE GET ERROR] cache={} key={}", cache.getName(), key, e);
                throw e; // 일단 원인 잡을 때는 throw
                // 운영에서는 return; (fail-open)로 바꿔도 됨
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.error("[CACHE PUT ERROR] cache={} key={} valueType={}",
                        cache.getName(), key, (value == null ? "null" : value.getClass().getName()), e);
                throw e;
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.error("[CACHE EVICT ERROR] cache={} key={}", cache.getName(), key, e);
                throw e;
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.error("[CACHE CLEAR ERROR] cache={}", cache.getName(), e);
                throw e;
            }
        };
    }
}
