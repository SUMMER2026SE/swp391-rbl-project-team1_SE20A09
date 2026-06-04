package com.sportvenue.event;

import com.sportvenue.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Handling notification event for user {}: {}", event.getUserId(), event.getTitle());
        try {
            notificationService.createNotification(
                    event.getUserId(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType(),
                    event.getRelatedResourceId()
            );
        } catch (Exception e) {
            log.error("Failed to create notification from event", e);
        }
    }
}
