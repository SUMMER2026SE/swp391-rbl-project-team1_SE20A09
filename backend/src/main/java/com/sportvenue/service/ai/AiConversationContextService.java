package com.sportvenue.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cache "kết quả vừa hiển thị" theo lượt chat — giải quyết câu hỏi kiểu "sân đầu tiên/thứ hai"
 * ở lượt sau bằng cách tra ID THẬT theo thứ tự đã hiển thị, thay vì để LLM tự đoán/bịa ID (xem
 * docs/ai_chatbot_rebuild_plan.md mục 6.2). Redis lỗi/timeout không được làm crash luồng chat
 * chính — mọi thao tác đọc/ghi đều nuốt lỗi và log warn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiConversationContextService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "ai_ctx:";
    private static final Duration TTL = Duration.ofMinutes(15);

    public enum LastShownType {
        STADIUM
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastShownResults {
        private LastShownType type;
        private List<Integer> ids;
    }

    public void saveLastShownStadiums(String conversationKey, List<Integer> stadiumIds) {
        if (conversationKey == null || stadiumIds == null || stadiumIds.isEmpty()) {
            return;
        }
        save(conversationKey, new LastShownResults(LastShownType.STADIUM, stadiumIds));
    }

    /** @param targetIndex 0-based, đúng thứ tự đã hiển thị cho người dùng ("sân đầu tiên" = 0). */
    public Optional<Integer> resolveStadiumIdByIndex(String conversationKey, int targetIndex) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey)
                .filter(r -> r.getType() == LastShownType.STADIUM && r.getIds() != null)
                .map(LastShownResults::getIds)
                .filter(ids -> targetIndex >= 0 && targetIndex < ids.size())
                .map(ids -> ids.get(targetIndex));
    }

    private void save(String conversationKey, LastShownResults results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationKey, json, TTL);
        } catch (Exception e) {
            log.warn("Không lưu được lastShownResults context (bỏ qua, không ảnh hưởng luồng chat chính): {}", e.getMessage());
        }
    }

    private Optional<LastShownResults> load(String conversationKey) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + conversationKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, LastShownResults.class));
        } catch (Exception e) {
            log.warn("Không đọc được lastShownResults context (bỏ qua, coi như chưa có sân nào hiển thị): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
