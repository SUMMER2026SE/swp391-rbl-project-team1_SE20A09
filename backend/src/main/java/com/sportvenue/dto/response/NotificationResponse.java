package com.sportvenue.dto.response;

import com.sportvenue.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private Boolean isRead;
    private String relatedResourceId;
    private LocalDateTime createdAt;
}
