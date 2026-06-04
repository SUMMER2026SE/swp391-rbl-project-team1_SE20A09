package com.sportvenue.event;

import com.sportvenue.entity.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEvent {
    private final Integer userId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final String relatedResourceId;
}
