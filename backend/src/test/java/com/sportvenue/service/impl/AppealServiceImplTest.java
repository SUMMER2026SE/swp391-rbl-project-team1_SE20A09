package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateAppealRequest;
import com.sportvenue.dto.request.ReviewAppealRequest;
import com.sportvenue.dto.response.AppealResponse;
import com.sportvenue.entity.Appeal;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.AppealStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.AppealRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AdminOwnerService;
import com.sportvenue.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealServiceImplTest {

    @Mock
    private AppealRepository appealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private AdminOwnerService adminOwnerService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppealServiceImpl appealService;

    @Test
    void createAppeal_BlockedUser_SavesPendingAppealAndNotifiesAdmins() {
        User user = user(10, "Customer", AccountStatus.BLOCKED);
        user.setLockReason("Violation");
        User admin = user(1, "Admin", AccountStatus.ACTIVE);

        CreateAppealRequest request = new CreateAppealRequest();
        request.setAppealText("Please review my account.");
        request.setEvidenceUrls(List.of(" https://example.com/a.png ", "https://example.com/a.png"));

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(appealRepository.existsByUserUserIdAndStatus(10, AppealStatus.PENDING)).thenReturn(false);
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> {
            Appeal appeal = invocation.getArgument(0);
            appeal.setAppealId(99);
            return appeal;
        });
        when(userRepository.findAllAdmins()).thenReturn(List.of(admin));

        AppealResponse response = appealService.createAppeal(request, new UserPrincipal(user));

        assertEquals(99, response.getAppealId());
        assertEquals(AppealStatus.PENDING, response.getStatus());
        assertEquals(List.of("https://example.com/a.png"), response.getEvidenceUrls());
        verify(notificationService).createNotification(
                eq(1),
                eq("Kháng cáo tài khoản mới"),
                any(),
                eq(NotificationType.APPEAL),
                eq("APPEAL-99"));
    }

    @Test
    void createAppeal_ActiveUser_ThrowsException() {
        User user = user(10, "Customer", AccountStatus.ACTIVE);
        when(userRepository.findById(10)).thenReturn(Optional.of(user));

        CreateAppealRequest request = new CreateAppealRequest();
        request.setAppealText("Please review.");

        assertThrows(BadRequestException.class, () ->
                appealService.createAppeal(request, new UserPrincipal(user)));
    }

    @Test
    void createAppeal_PendingAppealAlreadyExists_ThrowsException() {
        User user = user(10, "Customer", AccountStatus.BLOCKED);
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(appealRepository.existsByUserUserIdAndStatus(10, AppealStatus.PENDING)).thenReturn(true);

        CreateAppealRequest request = new CreateAppealRequest();
        request.setAppealText("Please review.");

        assertThrows(BadRequestException.class, () ->
                appealService.createAppeal(request, new UserPrincipal(user)));
    }

    @Test
    void reviewAppeal_Approved_UnlocksUserAndNotifiesUser() {
        User admin = user(1, "Admin", AccountStatus.ACTIVE);
        User lockedUser = user(10, "Customer", AccountStatus.BLOCKED);
        lockedUser.setLockReason("Violation");
        Appeal appeal = Appeal.builder()
                .appealId(99)
                .user(lockedUser)
                .appealText("Please review.")
                .status(AppealStatus.PENDING)
                .build();

        ReviewAppealRequest request = new ReviewAppealRequest();
        request.setStatus(AppealStatus.APPROVED);
        request.setAdminNote("Accepted");

        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(appealRepository.findById(99)).thenReturn(Optional.of(appeal));
        when(ownerRepository.findByUserUserId(10)).thenReturn(Optional.empty());
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppealResponse response = appealService.reviewAppeal(99, request, new UserPrincipal(admin));

        assertEquals(AppealStatus.APPROVED, response.getStatus());
        assertEquals(AccountStatus.ACTIVE, lockedUser.getAccountStatus());
        assertNull(lockedUser.getLockReason());
        verify(userRepository).save(lockedUser);
        verify(notificationService).createNotification(
                eq(10),
                eq("Kháng cáo đã được chấp nhận"),
                any(),
                eq(NotificationType.APPEAL),
                eq("APPEAL-99"));
    }

    @Test
    void reviewAppeal_ApprovedOwner_UsesOwnerUnlockCascade() {
        User admin = user(1, "Admin", AccountStatus.ACTIVE);
        User lockedOwnerUser = user(20, "Owner", AccountStatus.BLOCKED);
        lockedOwnerUser.setLockReason("Violation");
        Owner owner = Owner.builder()
                .ownerId(77)
                .user(lockedOwnerUser)
                .businessName("Owner Business")
                .build();
        Appeal appeal = Appeal.builder()
                .appealId(100)
                .user(lockedOwnerUser)
                .appealText("Please review.")
                .status(AppealStatus.PENDING)
                .build();

        ReviewAppealRequest request = new ReviewAppealRequest();
        request.setStatus(AppealStatus.APPROVED);
        request.setAdminNote("Accepted");

        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(appealRepository.findById(100)).thenReturn(Optional.of(appeal));
        when(ownerRepository.findByUserUserId(20)).thenReturn(Optional.of(owner));
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppealResponse response = appealService.reviewAppeal(100, request, new UserPrincipal(admin));

        assertEquals(AppealStatus.APPROVED, response.getStatus());
        verify(adminOwnerService).lockUnlockOwner(77, true, null);
        verify(notificationService).createNotification(
                eq(20),
                any(),
                any(),
                eq(NotificationType.APPEAL),
                eq("APPEAL-100"));
    }

    private User user(Integer id, String roleName, AccountStatus status) {
        return User.builder()
                .userId(id)
                .email("user" + id + "@example.com")
                .firstName("User")
                .lastName(String.valueOf(id))
                .role(Role.builder().roleName(roleName).build())
                .accountStatus(status)
                .build();
    }
}
