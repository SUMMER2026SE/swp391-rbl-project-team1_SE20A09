package com.sportvenue.service;

import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    PageResponse<NotificationResponse> getMyNotifications(Integer userId, Boolean unreadOnly, Pageable pageable);
    
    long countUnread(Integer userId);
    
    void markAsRead(Integer userId, List<Long> notificationIds);
    
    void markAllAsRead(Integer userId);
    
    void createNotification(Integer userId, String title, String message, NotificationType type, String relatedResourceId);
}
