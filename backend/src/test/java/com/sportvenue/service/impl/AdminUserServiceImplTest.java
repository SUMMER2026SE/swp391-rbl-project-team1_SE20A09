package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.UserRank;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AccountStatusHistoryService;
import com.sportvenue.service.AccountStatusService;
import com.sportvenue.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountStatusHistoryService accountStatusHistoryService;

    @org.mockito.Spy
    private AccountStatusService accountStatusService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("123456789")
                .avatarUrl("http://example.com/avatar.jpg")
                .accountStatus(AccountStatus.ACTIVE)
                .userRank(UserRank.BRONZE)
                .userPoint(100)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getCustomers_WithSearch_ReturnsFiltered() {
        // Arrange
        String search = "John";
        AccountStatus status = null;
        Page<User> userPage = new PageImpl<>(List.of(sampleUser));
        
        when(userRepository.findByRoleWithFilters(eq("Customer"), eq(search), eq(status), eq(pageable)))
                .thenReturn(userPage);

        // Act
        PageResponse<AdminCustomerResponse> result = adminUserService.getCustomers(search, status, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("John", result.getContent().get(0).getFirstName());
    }

    @Test
    void getCustomers_SearchNull_ReturnsAll() {
        // Arrange
        String search = null;
        AccountStatus status = null;
        Page<User> userPage = new PageImpl<>(List.of(sampleUser));
        
        when(userRepository.findByRoleWithFilters(eq("Customer"), eq(search), eq(status), eq(pageable)))
                .thenReturn(userPage);

        // Act
        PageResponse<AdminCustomerResponse> result = adminUserService.getCustomers(search, status, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getCustomers_WithAccountStatus_ReturnsFiltered() {
        // Arrange
        String search = null;
        AccountStatus status = AccountStatus.BLOCKED;
        
        User blockedUser = User.builder()
                .userId(2)
                .accountStatus(AccountStatus.BLOCKED)
                .build();
                
        Page<User> userPage = new PageImpl<>(List.of(blockedUser));
        
        when(userRepository.findByRoleWithFilters(eq("Customer"), eq(search), eq(status), eq(pageable)))
                .thenReturn(userPage);

        // Act
        PageResponse<AdminCustomerResponse> result = adminUserService.getCustomers(search, status, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(AccountStatus.BLOCKED, result.getContent().get(0).getAccountStatus());
    }

    @Test
    void getCustomers_MapsFieldsCorrectly() {
        // Arrange
        Page<User> userPage = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findByRoleWithFilters(any(), any(), any(), any()))
                .thenReturn(userPage);

        // Act
        PageResponse<AdminCustomerResponse> result = adminUserService.getCustomers(null, null, pageable);

        // Assert
        AdminCustomerResponse response = result.getContent().get(0);
        assertEquals(sampleUser.getUserId(), response.getUserId());
        assertEquals(sampleUser.getFirstName(), response.getFirstName());
        assertEquals(sampleUser.getLastName(), response.getLastName());
        assertEquals("John Doe", response.getFullName());
        assertEquals(sampleUser.getEmail(), response.getEmail());
        assertEquals(sampleUser.getPhoneNumber(), response.getPhoneNumber());
        assertEquals(sampleUser.getAvatarUrl(), response.getAvatarUrl());
        assertEquals(sampleUser.getAccountStatus(), response.getAccountStatus());
        assertEquals(sampleUser.getUserRank(), response.getUserRank());
        assertEquals(sampleUser.getUserPoint(), response.getUserPoint());
        assertEquals(sampleUser.getIsVerified(), response.getIsVerified());
        assertEquals(sampleUser.getCreatedAt(), response.getCreatedAt());
    }

    @Test
    void getCustomers_UserWithNullStatus_HandledGracefully() {
        // Arrange
        User nullStatusUser = User.builder()
                .userId(3)
                .firstName("Null")
                .lastName("Status")
                .accountStatus(null)
                .userRank(null)
                .build();
                
        Page<User> userPage = new PageImpl<>(List.of(nullStatusUser));
        when(userRepository.findByRoleWithFilters(any(), any(), any(), any()))
                .thenReturn(userPage);

        // Act
        PageResponse<AdminCustomerResponse> result = adminUserService.getCustomers(null, null, pageable);

        // Assert
        assertNotNull(result);
        AdminCustomerResponse response = result.getContent().get(0);
        assertNull(response.getAccountStatus());
        assertNull(response.getUserRank());
        assertEquals("Null Status", response.getFullName());
    }

    @Test
    void lockUnlockCustomer_selfLockProtection_throwsException() {
        com.sportvenue.exception.BadRequestException exception = assertThrows(com.sportvenue.exception.BadRequestException.class, () -> {
            adminUserService.lockUnlockCustomer(1, false, 1);
        });
        assertEquals("Bạn không thể tự khóa tài khoản của chính mình.", exception.getMessage());
    }

    @Test
    void lockUnlockCustomer_userNotFound_throwsException() {
        when(userRepository.findById(2)).thenReturn(java.util.Optional.empty());
        
        com.sportvenue.exception.AppException exception = assertThrows(com.sportvenue.exception.AppException.class, () -> {
            adminUserService.lockUnlockCustomer(2, false, 1);
        });
        assertEquals(com.sportvenue.exception.ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void lockUnlockCustomer_notCustomer_throwsException() {
        User adminUser = User.builder().userId(2).role(com.sportvenue.entity.Role.builder().roleName("Admin").build()).build();
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(adminUser));
        
        com.sportvenue.exception.ResourceNotFoundException exception = assertThrows(com.sportvenue.exception.ResourceNotFoundException.class, () -> {
            adminUserService.lockUnlockCustomer(2, false, 1);
        });
        assertEquals("Người dùng không phải là khách hàng (Customer).", exception.getMessage());
    }

    @Test
    void lockUnlockCustomer_success() {
        User customerUser = User.builder().userId(2).accountStatus(AccountStatus.ACTIVE).role(com.sportvenue.entity.Role.builder().roleName("Customer").build()).build();
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(customerUser));
        
        adminUserService.lockUnlockCustomer(2, false, 1);
        
        assertEquals(AccountStatus.BLOCKED, customerUser.getAccountStatus());
        org.mockito.Mockito.verify(userRepository).save(customerUser);
    }

    @Test
    void lockUnlockCustomer_pendingAccount_throwsExceptionAndDoesNotChangeStatus() {
        User customerUser = User.builder()
                .userId(2)
                .accountStatus(AccountStatus.PENDING)
                .role(com.sportvenue.entity.Role.builder().roleName("Customer").build())
                .build();
        when(userRepository.findById(2)).thenReturn(java.util.Optional.of(customerUser));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                adminUserService.lockUnlockCustomer(2, true, 1));

        assertEquals("Không thể khóa/mở khóa tài khoản đang chờ xác thực email.", exception.getMessage());
        assertEquals(AccountStatus.PENDING, customerUser.getAccountStatus());
        verify(userRepository, never()).save(any());
    }
}
