package com.sportvenue.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEmailEvent extends ApplicationEvent {
    private final Integer customerId;
    private final String eventType;
    private final Object relatedEntity;

    public NotificationEmailEvent(Object source, Integer customerId, String eventType, Object relatedEntity) {
        super(source);
        this.customerId = customerId;
        this.eventType = eventType;
        this.relatedEntity = relatedEntity;
    }
}
