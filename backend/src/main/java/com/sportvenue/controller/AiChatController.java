package com.sportvenue.controller;

import com.sportvenue.dto.request.AiChatRequest;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ai.AiChatService;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final Executor aiTaskExecutor;
    private final Map<String, RateLimitEntry> rateLimitBuckets = new ConcurrentHashMap<>();

    private static final int CUSTOMER_REQUESTS_PER_MINUTE = 10;
    private static final int GUEST_REQUESTS_PER_MINUTE = 5;
    private static final long BUCKET_IDLE_EXPIRY_MILLIS = Duration.ofMinutes(10).toMillis();

    public AiChatController(AiChatService aiChatService,
                            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.aiChatService = aiChatService;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    /** Bucket kèm timestamp lần truy cập gần nhất — dùng để dọn dẹp entry của IP/user không còn hoạt động. */
    private static final class RateLimitEntry {
        private final Bucket bucket;
        private volatile long lastAccessMillis;

        private RateLimitEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessMillis = System.currentTimeMillis();
        }
    }

    /**
     * Dọn các bucket không hoạt động quá 10 phút — tránh rateLimitBuckets phình vô hạn
     * khi có nhiều IP khách vãng lai khác nhau truy cập (memory leak trên singleton Bean).
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    void evictStaleRateLimitBuckets() {
        long now = System.currentTimeMillis();
        rateLimitBuckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccessMillis > BUCKET_IDLE_EXPIRY_MILLIS);
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public ResponseBodyEmitter chatStream(@Valid @RequestBody AiChatRequest request,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse response) {
        Integer userId = userPrincipal != null ? userPrincipal.getUserId() : null;

        // Rate limiting: user đã đăng nhập tính theo userId, guest tính theo IP
        // (data 3 tool hiện có đều public nên không cần bắt đăng nhập, nhưng vẫn
        // phải giới hạn riêng để tránh 1 IP ẩn danh spam free-tier Groq key).
        String rateLimitKey = userId != null ? "u:" + userId : "ip:" + getClientIp(httpRequest);
        int limitPerMinute = userId != null ? CUSTOMER_REQUESTS_PER_MINUTE : GUEST_REQUESTS_PER_MINUTE;

        RateLimitEntry entry = rateLimitBuckets.computeIfAbsent(rateLimitKey, key -> new RateLimitEntry(Bucket.builder()
                .addLimit(limit -> limit.capacity(limitPerMinute).refillGreedy(limitPerMinute, Duration.ofMinutes(1)))
                .build()));
        entry.lastAccessMillis = System.currentTimeMillis();

        if (!entry.bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for: {}", rateLimitKey);
            response.setStatus(429); // TOO_MANY_REQUESTS
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();
            try {
                emitter.send("3:\"Rate limit exceeded\"\n");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Set headers directly on HTTP response
        response.setHeader("X-Vercel-AI-Data-Stream", "v1");
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(300000L); // 5 minutes timeout

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        AtomicReference<Future<?>> futureRef = new AtomicReference<>();

        Runnable cleanup = () -> {
            Future<?> future = futureRef.get();
            if (future != null && !future.isDone()) {
                log.info("Cancelling task execution due to SSE abort for user: {}", userId);
                future.cancel(true);
            }
            InputStream is = activeStreamRef.get();
            if (is != null) {
                try {
                    log.info("Closing active HTTP stream due to SSE abort for user: {}", userId);
                    is.close();
                } catch (Exception e) {
                    log.warn("Error closing stream on abort", e);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.warn("SSE connection error for user: {}", userId, e);
            cleanup.run();
        });

        Future<?> future = CompletableFuture.runAsync(() -> {
            try {
                aiChatService.handleChatStream(request, emitter, userPrincipal, activeStreamRef);
            } catch (Exception e) {
                log.error("Unhandled error in task execution", e);
                emitter.completeWithError(e);
            }
        }, aiTaskExecutor);
        futureRef.set(future);

        return emitter;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
