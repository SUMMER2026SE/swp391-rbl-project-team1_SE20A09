package com.sportvenue.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Graceful degradation cho Redis cache.
 *
 * Khi Redis down, @Cacheable / @CacheEvict không ném exception lên caller —
 * operation bị miss và fallback về DB bình thường.
 * Đây là pattern "best-effort cache": hệ thống chậm hơn khi Redis down
 * nhưng không bị crash.
 */
@Configuration
@Slf4j
public class CacheErrorHandlerConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new RedisCacheErrorHandler();
    }

    private static class RedisCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
            log.warn("[Cache] GET miss (Redis unavailable) — cache={}, key={}: {}",
                    cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
            log.warn("[Cache] PUT failed (Redis unavailable) — cache={}, key={}: {}",
                    cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
            log.warn("[Cache] EVICT failed (Redis unavailable) — cache={}, key={}: {}",
                    cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException ex, Cache cache) {
            log.warn("[Cache] CLEAR failed (Redis unavailable) — cache={}: {}",
                    cache.getName(), ex.getMessage());
        }
    }
}
