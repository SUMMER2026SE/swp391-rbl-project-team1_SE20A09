package com.sportvenue.service.ai;

/**
 * Lỗi khi gọi LLM gateway (Groq) — mang theo {@link Kind} để AiChatServiceImpl phân nhánh
 * thông điệp/log phù hợp thay vì 1 catch-all chung chung cho mọi loại lỗi (401/429/timeout
 * đều rất khác nhau về nguyên nhân và cách người dùng nên phản ứng).
 */
public class LlmGatewayException extends RuntimeException {

    public enum Kind {
        /** API key sai/hết hạn/chưa cấu hình — lỗi cấu hình phía server, cần log riêng để alert. */
        AUTH_ERROR,
        /** Vượt rate limit/quota của Groq (theo phút hoặc theo ngày). */
        RATE_LIMITED,
        /** Timeout hoặc lỗi kết nối khi gọi Groq. */
        TIMEOUT,
        /** Không xác định được loại cụ thể. */
        UNKNOWN
    }

    private final Kind kind;

    public LlmGatewayException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public LlmGatewayException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
