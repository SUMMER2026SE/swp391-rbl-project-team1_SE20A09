package com.sportvenue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.response.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter áp dụng Rate Limiting cho các endpoint nhạy cảm (Register, Resend OTP).
 * Giới hạn: 5 requests / 1 giờ / 1 IP.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Chỉ áp dụng cho POST /api/v1/auth/register và /api/v1/auth/resend-otp
        if ("POST".equalsIgnoreCase(method) && 
            ("/api/v1/auth/register".equals(path) || "/api/v1/auth/resend-otp".equals(path))) {
            
            String ip = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(ip, this::createNewBucket);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                handleLimitExceeded(response, probe.getNanosToWaitForRefill());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createNewBucket(String key) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofHours(1)))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void handleLimitExceeded(HttpServletResponse response, long nanosToWait) throws IOException {
        long minutesToWait = nanosToWait / 1_000_000_000 / 60;
        if (minutesToWait == 0) minutesToWait = 1;

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Bạn đã thực hiện quá nhiều yêu cầu. Vui lòng thử lại sau " + minutesToWait + " phút."
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
