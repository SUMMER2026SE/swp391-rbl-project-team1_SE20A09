package com.sportvenue.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Request cho 1 lượt chat — kiến trúc đơn-JSON (xem docs/ai_chatbot_rebuild_plan.md). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatTurnRequest {

    @NotBlank(message = "message không được để trống")
    @Size(max = 500, message = "message không được vượt quá 500 ký tự")
    private String message;

    /** Lịch sử hội thoại (không bắt buộc) — dùng để LLM hiểu ngữ cảnh các lượt trước. */
    @Valid
    private List<ChatMessage> history;

    /**
     * Tọa độ GPS hiện tại của user (optional) — được Frontend gửi kèm khi user đã cấp quyền
     * vị trí. Dùng để tìm sân gần nhất khi Groq trả về location = "CURRENT_LOCATION".
     */
    private Double userLat;
    private Double userLng;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        @NotBlank
        private String role; // "user" | "assistant"

        @NotBlank
        private String content;
    }
}
