package com.sportvenue.service.impl;

import com.sportvenue.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final String PROCESSING = "PROCESSING";
    /** TTL khi đang xử lý — đủ để request hoàn tất, không để treo vĩnh viễn. */
    private static final Duration PROCESSING_TTL = Duration.ofSeconds(30);
    /** TTL sau khi hoàn thành — client có thể retry trong 24h và nhận lại cùng booking. */
    private static final Duration RESULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(Integer userId, String idempotencyKey) {
        String key = buildKey(userId, idempotencyKey);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, PROCESSING, PROCESSING_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void complete(Integer userId, String idempotencyKey, Integer bookingId) {
        String key = buildKey(userId, idempotencyKey);
        redisTemplate.opsForValue().set(key, String.valueOf(bookingId), RESULT_TTL);
        log.debug("[Idempotency] complete — key={}, bookingId={}", key, bookingId);
    }

    @Override
    public void release(Integer userId, String idempotencyKey) {
        String key = buildKey(userId, idempotencyKey);
        redisTemplate.delete(key);
        log.debug("[Idempotency] released — key={}", key);
    }

    @Override
    public Optional<Integer> getExistingBookingId(Integer userId, String idempotencyKey) {
        String key = buildKey(userId, idempotencyKey);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || PROCESSING.equals(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            log.warn("[Idempotency] giá trị không parse được — key={}, value={}", key, value);
            return Optional.empty();
        }
    }

    private String buildKey(Integer userId, String idempotencyKey) {
        return "idem:booking:" + userId + ":" + idempotencyKey;
    }
}
