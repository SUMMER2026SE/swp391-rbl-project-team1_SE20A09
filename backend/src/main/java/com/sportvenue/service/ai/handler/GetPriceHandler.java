package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPriceHandler {

    public AiChatTurnResponse handle(JsonNode args, String message) {
        // Có thể bổ sung query để lấy giá trung bình theo khu vực và môn học
        return AiChatTurnResponse.builder()
                .intent("get_price")
                .message("Đây là thông tin giá tham khảo theo yêu cầu của bạn:")
                .build();
    }
}
