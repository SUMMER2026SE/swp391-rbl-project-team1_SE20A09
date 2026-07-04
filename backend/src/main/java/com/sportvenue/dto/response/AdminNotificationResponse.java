package com.sportvenue.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sportvenue.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho thông báo hiển thị trên bell dropdown của Admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {

    private String id;
    private NotificationType type;
    private String title;
    private String description;
    private LocalDateTime createdAt;

    /**
     * Dùng @JsonProperty("isRead") để Jackson serialize đúng key "isRead"
     * thay vì bỏ prefix "is" với boolean primitive.
     */
    @JsonProperty("isRead")
    private boolean isRead;

    private Long notificationId;
}
