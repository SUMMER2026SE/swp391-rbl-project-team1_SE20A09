package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.AccountStatusHistoryService;
import com.sportvenue.service.AccountStatusService;
import com.sportvenue.service.EmailService;
import com.sportvenue.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOwnerServiceImplTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountStatusHistoryService accountStatusHistoryService;

    @org.mockito.Spy
    private AccountStatusService accountStatusService;

    @InjectMocks
    private AdminOwnerServiceImpl adminOwnerService;

    @Test
    void getOwners_Success() {
        String search = "test";
        String accountStatusStr = "ACTIVE";
        String approvedStatusStr = "APPROVED";
        int page = 0;
        int size = 10;
        String sortBy = "businessName";
        String sortDir = "asc";

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, "businessName"));

        AdminOwnerResponse responseDto = new AdminOwnerResponse();
        Page<AdminOwnerResponse> ownerPage = new PageImpl<>(List.of(responseDto), pageable, 1);

        when(ownerRepository.findOwnersForAdmin(eq(search), eq(AccountStatus.ACTIVE), eq(ApprovedStatus.APPROVED), eq(pageable)))
                .thenReturn(ownerPage);

        PageResponse<AdminOwnerResponse> result = adminOwnerService.getOwners(search, accountStatusStr, approvedStatusStr, page, size, sortBy, sortDir);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ownerRepository).findOwnersForAdmin(eq(search), eq(AccountStatus.ACTIVE), eq(ApprovedStatus.APPROVED), eq(pageable));
    }

    @Test
    void getOwners_InvalidStatus_HandledGracefully() {
        String search = null;
        String accountStatusStr = "INVALID_STATUS";
        String approvedStatusStr = "INVALID_STATUS";
        int page = 0;
        int size = 10;
        String sortBy = "createdAt";
        String sortDir = "desc";

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AdminOwnerResponse> ownerPage = new PageImpl<>(List.of(), pageable, 0);

        when(ownerRepository.findOwnersForAdmin(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(ownerPage);

        PageResponse<AdminOwnerResponse> result = adminOwnerService.getOwners(search, accountStatusStr, approvedStatusStr, page, size, sortBy, sortDir);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(ownerRepository).findOwnersForAdmin(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void lockUnlockOwner_Lock_Success() {
        Integer ownerId = 1;

        User user = new User();
        user.setEmail("owner@example.com");
        user.setAccountStatus(AccountStatus.ACTIVE);

        Owner owner = new Owner();
        owner.setApprovedStatus(ApprovedStatus.APPROVED);
        owner.setUser(user);
        owner.setBusinessName("Test Business");

        Stadium stadium = new Stadium();

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findByOwnerOwnerId(ownerId)).thenReturn(List.of(stadium));

        adminOwnerService.lockUnlockOwner(ownerId, false, "Vi phạm quy định");

        assertEquals(AccountStatus.BLOCKED, user.getAccountStatus());
        assertEquals("Vi phạm quy định", user.getLockReason());
        assertEquals(StadiumStatus.MAINTENANCE, stadium.getStadiumStatus());
        verify(stadiumRepository).saveAll(anyList());
    }

    @Test
    void lockUnlockOwner_Unlock_Success() {
        Integer ownerId = 1;

        User user = new User();
        user.setEmail("owner@example.com");
        user.setAccountStatus(AccountStatus.BLOCKED);

        Owner owner = new Owner();
        owner.setApprovedStatus(ApprovedStatus.APPROVED);
        owner.setUser(user);
        owner.setBusinessName("Test Business");

        Stadium stadium = new Stadium();
        stadium.setStadiumStatus(StadiumStatus.MAINTENANCE);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findByOwnerOwnerId(ownerId)).thenReturn(List.of(stadium));

        adminOwnerService.lockUnlockOwner(ownerId, true, null);

        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertEquals(StadiumStatus.AVAILABLE, stadium.getStadiumStatus());
        verify(stadiumRepository).saveAll(anyList());
    }

    @Test
    void lockUnlockOwner_Unlock_DoesNotReactivateAdminSuspendedStadium() {
        Integer ownerId = 1;

        User user = new User();
        user.setEmail("owner@example.com");
        user.setAccountStatus(AccountStatus.BLOCKED);

        Owner owner = new Owner();
        owner.setApprovedStatus(ApprovedStatus.APPROVED);
        owner.setUser(user);
        owner.setBusinessName("Test Business");

        Stadium ownerLockedStadium = new Stadium();
        ownerLockedStadium.setStadiumStatus(StadiumStatus.MAINTENANCE);

        Stadium adminSuspendedStadium = new Stadium();
        adminSuspendedStadium.setStadiumStatus(StadiumStatus.MAINTENANCE);
        adminSuspendedStadium.setAdminSuspended(true);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findByOwnerOwnerId(ownerId))
                .thenReturn(List.of(ownerLockedStadium, adminSuspendedStadium));

        adminOwnerService.lockUnlockOwner(ownerId, true, null);

        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertEquals(StadiumStatus.AVAILABLE, ownerLockedStadium.getStadiumStatus());
        assertEquals(StadiumStatus.MAINTENANCE, adminSuspendedStadium.getStadiumStatus());
        assertEquals(Boolean.TRUE, adminSuspendedStadium.getAdminSuspended());
        verify(stadiumRepository).saveAll(anyList());
    }

    @Test
    void lockUnlockOwner_OwnerNotFound_ThrowsException() {
        when(ownerRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> adminOwnerService.lockUnlockOwner(999, false, null));
    }

    @Test
    void lockUnlockOwner_OwnerNotApproved_ThrowsException() {
        Integer ownerId = 1;

        Owner owner = new Owner();
        owner.setApprovedStatus(ApprovedStatus.PENDING);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThrows(AppException.class, () -> adminOwnerService.lockUnlockOwner(ownerId, false, null));
    }

    @Test
    void lockUnlockOwner_PendingAccount_ThrowsExceptionAndDoesNotCascadeStadiums() {
        Integer ownerId = 1;

        User user = new User();
        user.setEmail("owner@example.com");
        user.setAccountStatus(AccountStatus.PENDING);

        Owner owner = new Owner();
        owner.setApprovedStatus(ApprovedStatus.APPROVED);
        owner.setUser(user);

        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                adminOwnerService.lockUnlockOwner(ownerId, true, null));

        assertEquals("Không thể khóa/mở khóa tài khoản đang chờ xác thực email.", exception.getMessage());
        assertEquals(AccountStatus.PENDING, user.getAccountStatus());
        verify(stadiumRepository, never()).findByOwnerOwnerId(ownerId);
        verify(stadiumRepository, never()).saveAll(anyList());
    }
}
