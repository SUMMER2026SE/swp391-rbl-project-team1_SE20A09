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
 * ở lượt sau bằng cách tra ID THẬT theo thứ tự đã hiển thị, thay vì để LLM tự đoán/bịa ID.
 * Đồng thời giữ lại currentStadiumId để các bước booking không cần LLM nhắc lại ID sân.
 * Redis lỗi/timeout không được làm crash luồng chat chính — mọi thao tác đọc/ghi đều nuốt lỗi và log warn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiConversationContextService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "ai_ctx:";
    private static final Duration TTL = Duration.ofMinutes(15);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingAction {
        private String intent;
        private java.util.Map<String, Object> data;
        private String missingField;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationContext {
        private List<Integer> lastShownStadiumIds;
        private List<Integer> lastShownMatchIds;
        private List<Integer> lastShownSlotIds;
        private Integer currentStadiumId;
        private PendingAction pendingAction;

        // Expanded context
        private String currentIntent;
        private String currentSport;
        private String currentDistrict;
        private String currentProvince; // Bug #3: Cần lưu province để ưu tiên khu vực người dùng nhắc
        private String currentDate;
        private String currentTime;
        private java.util.Map<String, Object> bookingDraft;
        private java.util.Map<String, Object> joinMatchDraft;
    }

    public void saveLastShownStadiums(String conversationKey, List<Integer> stadiumIds) {
        if (conversationKey == null || stadiumIds == null || stadiumIds.isEmpty()) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setLastShownStadiumIds(stadiumIds);
        save(conversationKey, ctx);
    }

    public void saveLastShownMatches(String conversationKey, List<Integer> matchIds) {
        if (conversationKey == null || matchIds == null || matchIds.isEmpty()) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setLastShownMatchIds(matchIds);
        save(conversationKey, ctx);
    }

    public void saveLastShownSlots(String conversationKey, List<Integer> slotIds) {
        if (conversationKey == null || slotIds == null || slotIds.isEmpty()) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setLastShownSlotIds(slotIds);
        save(conversationKey, ctx);
    }

    public void saveCurrentStadiumId(String conversationKey, Integer stadiumId) {
        if (conversationKey == null || stadiumId == null) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setCurrentStadiumId(stadiumId);
        save(conversationKey, ctx);
    }

    public Optional<Integer> getCurrentStadiumId(String conversationKey) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey).map(ConversationContext::getCurrentStadiumId);
    }

    public Optional<Integer> resolveStadiumIdByIndex(String conversationKey, int targetIndex) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey)
                .map(ConversationContext::getLastShownStadiumIds)
                .filter(ids -> ids != null && targetIndex >= 0 && targetIndex < ids.size())
                .map(ids -> ids.get(targetIndex));
    }

    public List<Integer> getLastShownStadiumIds(String conversationKey) {
        if (conversationKey == null) {
            return null;
        }
        return load(conversationKey).map(ConversationContext::getLastShownStadiumIds).orElse(null);
    }

    public Optional<Integer> resolveMatchIdByIndex(String conversationKey, int matchIndex) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey)
                .map(ConversationContext::getLastShownMatchIds)
                .filter(ids -> ids != null && matchIndex >= 0 && matchIndex < ids.size())
                .map(ids -> ids.get(matchIndex));
    }

    public Optional<Integer> resolveSlotIdByIndex(String conversationKey, int slotIndex) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey)
                .map(ConversationContext::getLastShownSlotIds)
                .filter(ids -> ids != null && slotIndex >= 0 && slotIndex < ids.size())
                .map(ids -> ids.get(slotIndex));
    }

    public void savePendingAction(String conversationKey, PendingAction pendingAction) {
        if (conversationKey == null) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setPendingAction(pendingAction);
        save(conversationKey, ctx);
    }

    public Optional<PendingAction> getPendingAction(String conversationKey) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey).map(ConversationContext::getPendingAction);
    }

    public void clearPendingAction(String conversationKey) {
        if (conversationKey == null) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setPendingAction(null);
        save(conversationKey, ctx);
    }

    // Expanded context methods
    public void saveCurrentFilters(String conversationKey, String sport, String district, String province, String date, String time) {
        if (conversationKey == null) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        if (sport != null) {
            ctx.setCurrentSport(sport);
        }
        if (district != null) {
            ctx.setCurrentDistrict(district);
        }
        if (province != null) {
            ctx.setCurrentProvince(province); // Bug #3: Lưu province để ưu tiên khu vực người dùng nhắc
        }
        if (date != null) {
            ctx.setCurrentDate(date);
        }
        if (time != null) {
            ctx.setCurrentTime(time);
        }
        save(conversationKey, ctx);
    }

    public Optional<ConversationContext> getContext(String conversationKey) {
        if (conversationKey == null) {
            return Optional.empty();
        }
        return load(conversationKey);
    }
    
    public void saveBookingDraft(String conversationKey, java.util.Map<String, Object> draft) {
        if (conversationKey == null) {
            return;
        }
        ConversationContext ctx = load(conversationKey).orElse(new ConversationContext());
        ctx.setBookingDraft(draft);
        save(conversationKey, ctx);
    }

    private void save(String conversationKey, ConversationContext results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationKey, json, TTL);
        } catch (Exception e) {
            log.warn("Không lưu được context (bỏ qua, không ảnh hưởng luồng chat chính): {}", e.getMessage());
        }
    }

    private Optional<ConversationContext> load(String conversationKey) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + conversationKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, ConversationContext.class));
        } catch (Exception e) {
            log.warn("Không đọc được context (hoặc sai schema cũ), reset context: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

