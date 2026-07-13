package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelBookingHandler {

    public AiChatTurnResponse handle(JsonNode args, String message, Integer userId) {
        if (userId == null) {
            return AiChatTurnResponse.builder()
                    .intent("need_more_info")
                    .message("Bạn cần đăng nhập để hủy sân. Vui lòng đăng nhập và thử lại nhé.")
                    .build();
        }

        return AiChatTurnResponse.builder()
                .intent("cancel_booking")
                .message("Đây là các đơn đặt sân sắp tới của bạn. Bạn muốn hủy đơn nào?")
                .build();
    }
}
