package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendTimeHandler {

    public AiChatTurnResponse handle(JsonNode args, String message) {
        return AiChatTurnResponse.builder()
                .intent("recommend_time")
                .message("Theo dữ liệu của hệ thống, các sân thường trống nhiều nhất vào buổi sáng (6h-9h) và đầu giờ chiều (13h-15h).")
                .build();
    }
}
