package com.sportvenue.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * Cấu hình TTL riêng cho từng cache name.
 * Default TTL 1 giờ được set trong application.yml; override ở đây nếu cần khác.
 */
@Configuration
public class CacheConfig {

    /**
     * adminDashboard — TTL 5 phút.
     * Cache bị evict sớm hơn nếu có Owner được duyệt / Complaint mới
     * thông qua @CacheEvict trong AdminDashboardServiceImpl.evictDashboardCache().
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration(
                        "adminDashboard",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .disableCachingNullValues()
                );
    }
}
