package com.sportvenue.repository;

import com.sportvenue.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUserUserId(Integer userId, Pageable pageable);
    
    Page<Notification> findByUserUserIdAndIsRead(Integer userId, Boolean isRead, Pageable pageable);
    
    long countByUserUserIdAndIsReadFalse(Integer userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.notificationId IN :ids")
    void markIdsAsRead(@Param("userId") Integer userId, @Param("ids") List<Long> ids);
}
