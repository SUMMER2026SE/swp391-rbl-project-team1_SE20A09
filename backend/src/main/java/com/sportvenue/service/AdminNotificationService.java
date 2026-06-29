package com.sportvenue.service;

import com.sportvenue.dto.response.AdminNotificationResponse;

import java.util.List;

/**
 * Service tổng hợp thông báo cho Admin bell dropdown.
 * Nguồn dữ liệu: chủ sân chờ duyệt, sân chờ duyệt, khiếu nại mới.
 */
public interface AdminNotificationService {

    /** Lấy danh sách thông báo tổng hợp cho Admin (tối đa 50 mục, mới nhất trước). */
    List<AdminNotificationResponse> getAdminNotifications(Integer adminUserId);

    /** Đếm số thông báo chưa đọc của Admin. */
    long countUnread(Integer adminUserId);

    /**
     * Đánh dấu một thông báo là đã đọc theo notificationId.
     * Nếu chưa có Notification record → tự động tạo rồi đánh dấu đã đọc.
     */
    void markAsRead(Integer adminUserId, Long notificationId);

    /** Đánh dấu tất cả thông báo Admin là đã đọc (tạo record mới nếu chưa có). */
    void markAllAsRead(Integer adminUserId);
}
