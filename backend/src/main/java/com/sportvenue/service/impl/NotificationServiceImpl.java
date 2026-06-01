package com.sportvenue.service.impl;

import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Notification;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.repository.NotificationRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Integer userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> notificationPage;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notificationPage = notificationRepository.findByUser_UserIdAndIsRead(userId, false, pageable);
        } else {
            notificationPage = notificationRepository.findByUser_UserId(userId, pageable);
        }

        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(notificationPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Integer userId, List<Long> notificationIds) {
        notificationRepository.markIdsAsRead(userId, notificationIds);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void createNotification(Integer userId, String title, String message, NotificationType type, String relatedResourceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .relatedResourceId(relatedResourceId)
                .build();
        
        notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", userId, title);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .relatedResourceId(notification.getRelatedResourceId())
                .build();
    }
}
