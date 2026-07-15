package com.sportvenue.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class Bucket4jRedisConfig {

    @Bean
    public ProxyManager<byte[]> bucket4jProxyManager(LettuceConnectionFactory lettuceConnectionFactory) {
        Object nativeClient = lettuceConnectionFactory.getNativeClient();

        if (nativeClient instanceof RedisClient redisClient) {
            return LettuceBasedProxyManager.builderFor(redisClient)
                    .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(24)))
                    .build();
        } else {
            throw new IllegalStateException("Lettuce Native Client is not an instance of RedisClient");
        }
    }
}
