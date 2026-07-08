package com.sportvenue.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.request.AiChatTurnRequest;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ai.handler.MatchRequestHandler;
import com.sportvenue.service.ai.handler.PolicyHandler;
import com.sportvenue.service.ai.handler.SlotAvailabilityHandler;
import com.sportvenue.service.ai.handler.StadiumSearchHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dispatcher trung tâm — 1 lần gọi Groq (JSON mode) mỗi lượt chat, parse ra
 * {@link ExtractedIntentResult} rồi dispatch sang đúng handler theo {@code intent}. Thay cho
 * kiến trúc multi-turn tool-calling cũ (nhánh ai-chatting đã bỏ) — xem
 * docs/ai_chatbot_rebuild_plan.md để biết lý do đổi kiến trúc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final GroqClient groqClient;
    private final StadiumSearchHandler stadiumSearchHandler;
    private final SlotAvailabilityHandler slotAvailabilityHandler;
    private final MatchRequestHandler matchRequestHandler;
    private final PolicyHandler policyHandler;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${app.ai.model:llama-3.3-70b-versatile}")
    private String model;

    private Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

    void setClock(Clock clock) {
        this.clock = clock;
    }

    // Nội dung đầy đủ + rationale từng luật nằm ở backend/src/main/resources/prompts/customer/
    // — README.md trong thư mục đó giải thích lý do từng đoạn tồn tại. Sửa câu chữ prompt thì
    // sửa file .md, không sửa ở đây.
    private static final String CUSTOMER_SYSTEM_PROMPT = PromptLoader.load("prompts/customer/system-prompt.md");
    private static final String FAQ_PROMPT = PromptLoader.load("prompts/customer/faq.md");
    private static final String GUEST_SYSTEM_PROMPT_SUFFIX = PromptLoader.load("prompts/customer/guest-suffix.md");
    private static final String LOGGED_IN_SYSTEM_PROMPT_SUFFIX = PromptLoader.load("prompts/customer/logged-in-suffix.md");

    private static final String FALLBACK_MESSAGE =
            "Xin lỗi, tôi chưa giải quyết được vấn đề của bạn. Vui lòng liên hệ CSKH qua Hotline: 1900 xxxx hoặc Zalo SportHub để được hỗ trợ trực tiếp.";

    @Override
    public AiChatTurnResponse handleChat(AiChatTurnRequest request, UserPrincipal userPrincipal, String conversationKey) {
        Integer userId = userPrincipal != null ? userPrincipal.getUserId() : null;
        String systemPrompt = buildSystemPrompt(userId);
        List<GroqClient.ChatMessage> history = toGroqHistory(request.getHistory());

        ExtractedIntentResult intentResult;
        try {
            GroqClient.GroqResult result = groqClient.chatJson(model, systemPrompt, history, request.getMessage());
            log.debug("Groq raw JSON cho message '{}': {}", request.getMessage(), result.text());
            intentResult = objectMapper.readValue(result.text(), ExtractedIntentResult.class);
            log.info("Intent nhận diện: '{}' -> '{}'", request.getMessage(), intentResult.getIntent());
        } catch (LlmGatewayException e) {
            log.error("Lỗi gọi Groq gateway (kind={}): {}", e.getKind(), e.getMessage());
            return AiChatTurnResponse.messageOnly(FALLBACK_MESSAGE, "unknown");
        } catch (Exception e) {
            // Groq JSON Mode chỉ đảm bảo output là JSON hợp lệ, KHÔNG đảm bảo đúng schema —
            // model có thể bỏ sót field/trả sai kiểu. Không để lỗi parse crash cả request.
            log.warn("Không parse được JSON intent từ Groq, dùng fallback unknown", e);
            intentResult = ExtractedIntentResult.unknown();
        }

        return dispatch(intentResult, conversationKey);
    }

    private AiChatTurnResponse dispatch(ExtractedIntentResult result, String conversationKey) {
        String intent = result.getIntent();
        String message = result.getMessage();
        return switch (intent) {
            case "search_stadiums" -> stadiumSearchHandler.handle(result.getParams(), message, conversationKey);
            case "get_slots" -> slotAvailabilityHandler.handle(result.getParams(), message, conversationKey);
            case "find_match" -> matchRequestHandler.handle(result.getParams(), message);
            case "get_policy" -> policyHandler.handle(result.getParams(), message);
            case "need_more_info", "out_of_scope" ->
                    AiChatTurnResponse.messageOnly(message.isBlank() ? FALLBACK_MESSAGE : message, intent);
            default -> AiChatTurnResponse.messageOnly(FALLBACK_MESSAGE, "unknown");
        };
    }

    private List<GroqClient.ChatMessage> toGroqHistory(List<AiChatTurnRequest.ChatMessage> history) {
        if (history == null) {
            return List.of();
        }
        List<GroqClient.ChatMessage> converted = new ArrayList<>();
        for (AiChatTurnRequest.ChatMessage turn : history) {
            converted.add(new GroqClient.ChatMessage(turn.getRole(), turn.getContent()));
        }
        return converted;
    }

    private String buildSystemPrompt(Integer userId) {
        String roleSuffix = userId != null ? LOGGED_IN_SYSTEM_PROMPT_SUFFIX : GUEST_SYSTEM_PROMPT_SUFFIX;
        // Nối rõ ràng bằng " " thay vì dựa vào khoảng trắng đầu/cuối ẩn trong từng file .md
        // (PromptLoader.load() đã strip() từng đoạn).
        return String.join(" ", CUSTOMER_SYSTEM_PROMPT, FAQ_PROMPT, buildCurrentTimeContext(), roleSuffix);
    }

    /**
     * Model không biết "hôm nay" là ngày nào — thiếu dòng này thì "tối mai", "thứ 7 tuần này"
     * sẽ bị đoán sai ngày khi điền tham số date/targetDate.
     */
    private String buildCurrentTimeContext() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        String dayOfWeekVi = today.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, Locale.of("vi", "VN"));
        return "Bây giờ là " + now.format(DateTimeFormatter.ofPattern("HH:mm"))
                + " " + dayOfWeekVi + ", ngày " + today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + " (giờ Việt Nam). Hãy dùng mốc này để quy đổi 'hôm nay', 'ngày mai', 'tối nay', 'cuối tuần'... sang ngày YYYY-MM-DD khi điền params.";
    }
}
