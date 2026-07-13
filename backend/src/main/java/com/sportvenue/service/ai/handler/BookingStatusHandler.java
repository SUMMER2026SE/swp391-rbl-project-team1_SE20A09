package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingStatusHandler {

    public AiChatTurnResponse handle(JsonNode args, String message, Integer userId) {
        if (userId == null) {
            return AiChatTurnResponse.builder()
                    .intent("need_more_info")
                    .message("Bạn cần đăng nhập để kiểm tra trạng thái đơn đặt sân. Vui lòng đăng nhập và thử lại nhé.")
                    .build();
        }

        return AiChatTurnResponse.builder()
                .intent("booking_status")
                .message("Đây là trạng thái các đơn đặt sân gần đây của bạn:")
                .build();
    }
}
