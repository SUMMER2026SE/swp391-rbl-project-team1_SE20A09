package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminNotificationResponse;
import com.sportvenue.entity.Notification;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.repository.NotificationRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Đọc thông báo cho Admin trực tiếp từ bảng notifications.
 *
 * Notification records được tạo tại nguồn khi event xảy ra:
 *   - OWNER_APPROVAL : OwnerRegistrationServiceImpl.registerNewOwner / upgradeCurrentCustomer
 *   - STADIUM_APPROVAL: StadiumServiceImpl.createStadium
 *   - COMPLAINT       : ComplaintServiceImpl.createComplaint
 *
 * Việc mark-as-read chỉ đơn giản là UPDATE notifications SET is_read = true.
 * Thông báo vẫn được giữ lại vĩnh viễn sau khi đọc (không xóa).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final List<NotificationType> ADMIN_TYPES = List.of(
            NotificationType.OWNER_APPROVAL,
            NotificationType.STADIUM_APPROVAL,
            NotificationType.COMPLAINT
    );

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> getAdminNotifications(Integer adminUserId) {
        List<Notification> records = notificationRepository
                .findByUserUserIdAndNotificationTypeIn(adminUserId, ADMIN_TYPES);

        log.info("getAdminNotifications: userId={}, found {} records", adminUserId, records.size());
        records.forEach(n -> log.info("  -> notifId={}, type={}, resourceId={}, isRead={}",
                n.getNotificationId(), n.getNotificationType(), n.getRelatedResourceId(), n.getIsRead()));

        return records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Integer adminUserId) {
        return notificationRepository
                .findByUserUserIdAndNotificationTypeIn(adminUserId, ADMIN_TYPES)
                .stream()
                .filter(n -> Boolean.FALSE.equals(n.getIsRead()))
                .count();
    }

    @Override
    @Transactional
    public void markAsRead(Integer adminUserId, Long notificationId) {
        // Dùng bulk UPDATE query để tránh Hibernate flush/cache issues
        notificationRepository.markNotificationAsRead(notificationId);
        log.info("Marked notification {} as read", notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer adminUserId) {
        // Dùng bulk UPDATE query thay vì saveAll để tránh Hibernate flush issues
        notificationRepository.markAllAdminNotificationsAsRead(adminUserId, ADMIN_TYPES);
        log.info("Bulk marked all admin notifications as read for userId={}", adminUserId);
    }

    /**
     * Đánh dấu đã đọc bằng notificationId — dùng từ controller khi frontend
     * đã có notificationId (trường hợp item đã có record từ trước).
     */
    @Transactional
    public Long markAsReadByResourceId(Integer adminUserId, String resourceId,
                                       String title, String description, NotificationType type) {
        // Tìm notification theo resourceId cho admin này
        List<Notification> existing = notificationRepository
                .findByUserUserIdAndRelatedResourceId(adminUserId, resourceId);

        if (!existing.isEmpty()) {
            Notification keeper = existing.get(0);
            // Dùng bulk UPDATE thay vì save() để tránh Hibernate cache issues
            notificationRepository.markNotificationAsRead(keeper.getNotificationId());
            // Xóa duplicate nếu có
            if (existing.size() > 1) {
                notificationRepository.deleteAll(existing.subList(1, existing.size()));
            }
            return keeper.getNotificationId();
        }

        // Không tìm thấy — không nên xảy ra vì records được tạo khi event,
        // nhưng tạo mới để đảm bảo an toàn
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Notification n = Notification.builder()
                .user(adminUser)
                .notificationType(type)
                .title(title)
                .message(description)
                .relatedResourceId(resourceId)
                .isRead(true)
                .build();
        return notificationRepository.save(n).getNotificationId();
    }

    private AdminNotificationResponse toResponse(Notification n) {
        return AdminNotificationResponse.builder()
                .id(n.getRelatedResourceId())
                .type(n.getNotificationType())
                .title(n.getTitle())
                .description(n.getMessage())
                .createdAt(n.getCreatedAt())
                .isRead(Boolean.TRUE.equals(n.getIsRead()))
                .notificationId(n.getNotificationId())
                .build();
    }
}
