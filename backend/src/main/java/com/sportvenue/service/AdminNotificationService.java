package com.sportvenue.service;

import com.sportvenue.dto.response.AdminNotificationResponse;
import com.sportvenue.entity.enums.NotificationType;

import java.util.List;

/**
 * Service tổng hợp thông báo cho Admin bell dropdown.
 */
public interface AdminNotificationService {

    List<AdminNotificationResponse> getAdminNotifications(Integer adminUserId);

    long countUnread(Integer adminUserId);

    void markAsRead(Integer adminUserId, Long notificationId);

    void markAllAsRead(Integer adminUserId);

    /**
     * Đánh dấu đã đọc bằng resourceId — dùng khi frontend chưa có notificationId.
     * Tìm record hiện có theo resourceId; nếu không có thì tạo mới isRead=true.
     */
    Long markAsReadByResourceId(Integer adminUserId, String resourceId,
                                String title, String description, NotificationType type);
}
