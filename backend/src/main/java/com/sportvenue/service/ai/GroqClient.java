package com.sportvenue.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Client gọi Groq (OpenAI-compatible) — 1 lệnh gọi blocking/lượt chat, JSON mode bật để đảm bảo
 * output là JSON hợp lệ. Không hỗ trợ tool-calling/streaming — kiến trúc mới dùng đơn-JSON theo
 * lượt thay cho multi-turn tool-calling (xem docs/ai_chatbot_rebuild_plan.md).
 */
@Slf4j
@Component
public class GroqClient {

    private final RestClient restClient;

    public GroqClient(@Value("${app.ai.base-url}") String baseUrl,
                       @Value("${app.ai.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public record ChatMessage(String role, String content) {
    }

    public record GroqResult(String text, int inputTokens, int outputTokens, int totalTokens) {
    }

    /**
     * Gọi Groq với JSON mode bật + lịch sử hội thoại — trả về JSON thô (caller tự parse thành
     * {@link ExtractedIntentResult}).
     */
    public GroqResult chatJson(String model, String systemPrompt, List<ChatMessage> history, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(new ChatMessage("user", userMessage));

        JsonChatRequest requestBody = new JsonChatRequest(model, messages, 0.1, new ResponseFormat("json_object"));

        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new LlmGatewayException(LlmGatewayException.Kind.AUTH_ERROR,
                    "Groq API key sai hoặc chưa cấu hình (GROQ_API_KEY)", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new LlmGatewayException(LlmGatewayException.Kind.RATE_LIMITED,
                    "Groq đã vượt giới hạn tốc độ (rate limit)", e);
        } catch (RestClientException e) {
            throw new LlmGatewayException(LlmGatewayException.Kind.TIMEOUT,
                    "Không gọi được Groq API: " + e.getMessage(), e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmGatewayException(LlmGatewayException.Kind.UNKNOWN, "Groq trả về phản hồi rỗng/không hợp lệ");
        }

        String text = response.choices().get(0).message().content();
        Usage usage = response.usage();
        return new GroqResult(
                text,
                usage != null ? usage.promptTokens() : 0,
                usage != null ? usage.completionTokens() : 0,
                usage != null ? usage.totalTokens() : 0
        );
    }

    // ─── Internal request/response DTOs ──────────────────────────────────────

    record JsonChatRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
    }

    record ResponseFormat(String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }
}
