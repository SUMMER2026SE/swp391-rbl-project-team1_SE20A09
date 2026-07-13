package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyBookingsHandler {

    public AiChatTurnResponse handle(JsonNode args, String message, Integer userId) {
        if (userId == null) {
            return AiChatTurnResponse.builder()
                    .intent("need_more_info")
                    .message("Bạn cần đăng nhập để xem danh sách sân đã đặt. Vui lòng đăng nhập và thử lại nhé.")
                    .build();
        }

        // TODO: Gọi BookingService để lấy danh sách booking của user trong tuần
        // Hiện tại trả về frontend intent "my_bookings" để frontend tự render components phù hợp (gọi API riêng nếu cần)
        return AiChatTurnResponse.builder()
                .intent("my_bookings")
                .message("Đây là danh sách các sân bạn đã đặt gần đây:")
                .build();
    }
}
