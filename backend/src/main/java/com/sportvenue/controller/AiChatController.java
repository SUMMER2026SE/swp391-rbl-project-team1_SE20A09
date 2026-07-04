package com.sportvenue.controller;

import com.sportvenue.dto.request.AiChatRequest;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ai.AiChatService;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import jakarta.servlet.http.HttpServletResponse;

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
    private final Map<Integer, Bucket> userBuckets = new ConcurrentHashMap<>();

    public AiChatController(AiChatService aiChatService, 
                            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.aiChatService = aiChatService;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public ResponseBodyEmitter chatStream(@RequestBody AiChatRequest request,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal,
                                         HttpServletResponse response) {
        if (userPrincipal == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();
            try {
                emitter.send("3:\"Unauthorized\"\n");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        Integer userId = userPrincipal.getUserId();
        
        // Rate limiting: 10 requests / 1 minute / user
        Bucket bucket = userBuckets.computeIfAbsent(userId, id -> Bucket.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
                .build());

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for user: {}", userId);
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
}
