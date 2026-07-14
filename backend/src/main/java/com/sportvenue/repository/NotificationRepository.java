package com.sportvenue.repository;

import com.sportvenue.entity.Notification;
import com.sportvenue.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUserUserId(Integer userId, Pageable pageable);
    
    Page<Notification> findByUserUserIdAndIsRead(Integer userId, Boolean isRead, Pageable pageable);
    
    long countByUserUserIdAndIsReadFalse(Integer userId);

    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.notificationType IN (:types) ORDER BY n.createdAt DESC")
    Page<Notification> findCustomerNotifications(@Param("userId") Integer userId, @Param("types") List<NotificationType> types, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.notificationType IN (:types) AND n.isRead = false")
    long countCustomerUnread(@Param("userId") Integer userId, @Param("types") List<NotificationType> types);

    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.notificationType IN (:types) ORDER BY n.createdAt DESC")
    List<Notification> findCustomerNotificationsByType(@Param("userId") Integer userId, @Param("types") List<NotificationType> types);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.notificationId IN :ids")
    void markIdsAsRead(@Param("userId") Integer userId, @Param("ids") List<Long> ids);

    /**
     * Tìm notification theo user, type và relatedResourceId — dùng để kiểm tra trạng thái đọc.
     */
    Optional<Notification> findByUserUserIdAndNotificationTypeAndRelatedResourceId(
            Integer userId, NotificationType notificationType, String relatedResourceId);

    /**
     * Lấy tất cả notification của Admin theo danh sách type — dùng cho admin bell dropdown.
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user.userId = :userId
            AND n.notificationType IN :types
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserUserIdAndNotificationTypeIn(
            @Param("userId") Integer userId,
            @Param("types") List<NotificationType> types);

    /**
     * Tìm tất cả notification theo resourceId bất kể type — dùng để dedup khi mark-as-read.
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user.userId = :userId
            AND n.relatedResourceId = :resourceId
            ORDER BY n.notificationId ASC
            """)
    List<Notification> findByUserUserIdAndRelatedResourceId(
            @Param("userId") Integer userId,
            @Param("resourceId") String resourceId);

    /**
     * Bulk UPDATE: đánh dấu một notification cụ thể là đã đọc.
     * Dùng @Modifying để bypass Hibernate first-level cache.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId")
    void markNotificationAsRead(@Param("notificationId") Long notificationId);

    /**
     * Bulk UPDATE: đánh dấu tất cả notification của admin (theo type list) là đã đọc.
     */
    @Modifying
    @Query("""
            UPDATE Notification n SET n.isRead = true
            WHERE n.user.userId = :userId
            AND n.notificationType IN :types
            AND n.isRead = false
            """)
    void markAllAdminNotificationsAsRead(
            @Param("userId") Integer userId,
            @Param("types") List<NotificationType> types);

    /**
     * Đếm số thông báo chưa đọc của Admin tại tầng DB — tránh load toàn bộ records vào memory.
     */
    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.user.userId = :userId
            AND n.notificationType IN :types
            AND n.isRead = false
            """)
    long countUnreadAdminNotifications(
            @Param("userId") Integer userId,
            @Param("types") List<NotificationType> types);
}
