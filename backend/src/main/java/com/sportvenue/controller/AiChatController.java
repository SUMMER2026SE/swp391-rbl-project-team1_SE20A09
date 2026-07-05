package com.sportvenue.controller;

import com.sportvenue.dto.request.AiChatRequest;
import com.sportvenue.dto.request.AiFeedbackRequest;
import com.sportvenue.entity.AiChatFeedback;
import com.sportvenue.repository.AiChatFeedbackRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ai.AiChatService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final ProxyManager<byte[]> proxyManager;
    private final AiChatFeedbackRepository aiChatFeedbackRepository;

    private static final int CUSTOMER_REQUESTS_PER_MINUTE = 10;
    private static final int CUSTOMER_REQUESTS_PER_DAY = 150;
    private static final int GUEST_REQUESTS_PER_MINUTE = 5;
    private static final int GUEST_REQUESTS_PER_DAY = 50;

    public AiChatController(AiChatService aiChatService,
                            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor,
                            ProxyManager<byte[]> proxyManager,
                            AiChatFeedbackRepository aiChatFeedbackRepository) {
        this.aiChatService = aiChatService;
        this.aiTaskExecutor = aiTaskExecutor;
        this.proxyManager = proxyManager;
        this.aiChatFeedbackRepository = aiChatFeedbackRepository;
    }

    private BucketConfiguration getCustomerConfig() {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(CUSTOMER_REQUESTS_PER_MINUTE).refillGreedy(CUSTOMER_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
                .addLimit(limit -> limit.capacity(CUSTOMER_REQUESTS_PER_DAY).refillGreedy(CUSTOMER_REQUESTS_PER_DAY, Duration.ofDays(1)))
                .build();
    }

    private BucketConfiguration getGuestConfig() {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(GUEST_REQUESTS_PER_MINUTE).refillGreedy(GUEST_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
                .addLimit(limit -> limit.capacity(GUEST_REQUESTS_PER_DAY).refillGreedy(GUEST_REQUESTS_PER_DAY, Duration.ofDays(1)))
                .build();
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public ResponseBodyEmitter chatStream(@Valid @RequestBody AiChatRequest request,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse response) {
        Integer userId = userPrincipal != null ? userPrincipal.getUserId() : null;

        String sessionId = httpRequest.getHeader("X-Session-ID");
        String rateLimitKey = userId != null ? "ai_rate_limit:u:" + userId 
                            : (sessionId != null && !sessionId.isBlank() ? "ai_rate_limit:s:" + sessionId : "ai_rate_limit:ip:" + getClientIp(httpRequest));
                            
        BucketConfiguration bucketConfig = userId != null ? getCustomerConfig() : getGuestConfig();
        Bucket bucket = proxyManager.builder().build(rateLimitKey.getBytes(StandardCharsets.UTF_8), bucketConfig);

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for: {}", rateLimitKey);
            response.setStatus(429); // TOO_MANY_REQUESTS
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();
            try {
                emitter.send("3:\"Rate limit exceeded - Quá tải yêu cầu hoặc bạn đã hết lượt chat hôm nay. Vui lòng thử lại sau.\"\n");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        response.setHeader("X-Vercel-AI-Data-Stream", "v1");
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        ResponseBodyEmitter emitter = new ResponseBodyEmitter(300000L); // 5 minutes timeout

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        AtomicReference<Future<?>> futureRef = new AtomicReference<>();

        Runnable cleanup = () -> {
            Future<?> future = futureRef.get();
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            InputStream is = activeStreamRef.get();
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    log.warn("Error closing stream on abort", e);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.warn("SSE connection error for user: {}", userId != null ? userId : rateLimitKey, e);
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

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@Valid @RequestBody AiFeedbackRequest feedbackRequest,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal,
                                            HttpServletRequest request) {
        Integer userId = userPrincipal != null ? userPrincipal.getUserId() : null;
        String sessionId = request.getHeader("X-Session-ID");

        AiChatFeedback feedback = AiChatFeedback.builder()
                .messageId(feedbackRequest.getMessageId())
                .rating(feedbackRequest.getRating())
                .userId(userId)
                .sessionId(sessionId)
                .build();

        aiChatFeedbackRepository.save(feedback);
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
