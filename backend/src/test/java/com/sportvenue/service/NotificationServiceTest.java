package com.sportvenue.service;

import com.sportvenue.dto.response.NotificationResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Notification;
import com.sportvenue.entity.User;
import com.sportvenue.repository.NotificationRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User mockUser;
    private Integer testUserId = 1;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(testUserId);
    }

    @Test
    void getMyNotifications_ShouldReturnPageResponse() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = Notification.builder()
                .notificationId(1L)
                .user(mockUser)
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .build();
        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));

        when(notificationRepository.findByUserUserId(eq(testUserId), any(Pageable.class))).thenReturn(page);

        // When
        PageResponse<NotificationResponse> result = notificationService.getMyNotifications(testUserId, false, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Title", result.getContent().get(0).getTitle());
        verify(notificationRepository).findByUserUserId(testUserId, pageable);
    }

    @Test
    void markAsRead_ShouldCallRepository() {
        // When
        notificationService.markAsRead(testUserId, Collections.singletonList(1L));

        // Then
        verify(notificationRepository).markIdsAsRead(testUserId, Collections.singletonList(1L));
    }
}
