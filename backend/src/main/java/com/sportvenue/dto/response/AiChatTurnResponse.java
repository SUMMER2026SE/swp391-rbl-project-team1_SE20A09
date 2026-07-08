package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response cho 1 lượt chat. Chỉ đúng 1 trong 4 field kết quả (stadiums/slots/matches/policyText)
 * được populate tuỳ theo {@link #intent} — Frontend render card tương ứng, không parse tag từ
 * {@link #message} (xem docs/ai_chatbot_rebuild_plan.md mục 2A).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatTurnResponse {

    /** Lời thoại tự nhiên hiển thị trong khung chat. */
    private String message;

    /** Intent đã nhận diện — FE dùng để quyết định render card loại nào. */
    private String intent;

    private List<StadiumResponse> stadiums;

    private List<TimeSlotResponse> slots;

    private List<MatchResponse> matches;

    private String policyText;

    public static AiChatTurnResponse messageOnly(String message, String intent) {
        return AiChatTurnResponse.builder().message(message).intent(intent).build();
    }
}
