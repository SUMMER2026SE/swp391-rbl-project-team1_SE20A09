package com.sportvenue.service.impl;

import com.sportvenue.dto.request.ApproveOwnerRequest;
import com.sportvenue.dto.request.RegisterOwnerRequest;
import com.sportvenue.dto.request.UpgradeToOwnerRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.response.OwnerDetailResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.entity.enums.UserRank;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AdminDashboardService;
import com.sportvenue.service.OtpService;
import com.sportvenue.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerRegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdminDashboardService adminDashboardService;

    @Mock
    private com.sportvenue.service.EmailService emailService;

    @Mock
    private com.sportvenue.util.AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private OwnerRegistrationServiceImpl ownerRegistrationService;

    private Role ownerRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        ownerRole = new Role(2, "Owner");
        customerRole = new Role(1, "Customer");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String username) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void registerNewOwner_Success() {
        RegisterOwnerRequest request = RegisterOwnerRequest.builder()
                .email("newowner@example.com")
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .password("Password123!")
                .confirmPassword("Password123!")
                .businessName("San bong A")
                .taxCode("0123456789")
                .businessAddress("Hanoi")
                .businessLicenseUrl("http://example.com/license.jpg")
                .identityCardUrl("http://example.com/id.jpg")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhone())).thenReturn(false);
        when(roleRepository.findByRoleName("Owner")).thenReturn(Optional.of(ownerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .userId(100)
                .email(request.getEmail())
                .firstName("Nguyen")
                .lastName("Van A")
                .role(ownerRole)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ownerRegistrationService.registerNewOwner(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("Nguyen", capturedUser.getFirstName());
        assertEquals("Van A", capturedUser.getLastName());
        assertEquals("encodedPassword", capturedUser.getPasswordHash());
        assertEquals(AccountStatus.PENDING, capturedUser.getAccountStatus());
        assertFalse(capturedUser.getIsVerified());
        assertEquals(UserRank.BRONZE, capturedUser.getUserRank());

        ArgumentCaptor<Owner> ownerCaptor = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).save(ownerCaptor.capture());
        Owner capturedOwner = ownerCaptor.getValue();
        assertEquals("San bong A", capturedOwner.getBusinessName());
        assertEquals("0123456789", capturedOwner.getTaxCode());
        assertEquals("Hanoi", capturedOwner.getBusinessAddress());
        assertEquals("http://example.com/license.jpg", capturedOwner.getBusinessLicenseUrl());
        assertEquals("http://example.com/id.jpg", capturedOwner.getIdentityCardUrl());
        assertEquals(ApprovedStatus.PENDING, capturedOwner.getApprovedStatus());

        verify(otpService).createAndSendOtp(savedUser);
    }

    @Test
    void registerNewOwner_SingleWordName_LastNameShouldBeEmpty() {
        RegisterOwnerRequest request = RegisterOwnerRequest.builder()
                .email("single@example.com")
                .fullName("Huy")
                .phone("0987654321")
                .password("Password123!")
                .confirmPassword("Password123!")
                .businessName("San Huy")
                .taxCode("0123456789")
                .businessAddress("HCM")
                .businessLicenseUrl("http://example.com/license.jpg")
                .identityCardUrl("http://example.com/id.jpg")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.getPhone())).thenReturn(false);
        when(roleRepository.findByRoleName("Owner")).thenReturn(Optional.of(ownerRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ownerRegistrationService.registerNewOwner(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Huy", saved.getFirstName());
        assertEquals("", saved.getLastName());
    }

    @Test
    void registerNewOwner_DuplicateEmail_ThrowsException() {
        RegisterOwnerRequest request = RegisterOwnerRequest.builder()
                .email("owner@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                ownerRegistrationService.registerNewOwner(request));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerNewOwner_PasswordMismatch_ThrowsException() {
        RegisterOwnerRequest request = RegisterOwnerRequest.builder()
                .email("owner@example.com")
                .password("Password123!")
                .confirmPassword("WrongPassword123!")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ownerRegistrationService.registerNewOwner(request));

        assertEquals("Mật khẩu xác nhận không khớp", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void upgradeCurrentCustomer_Success_FirstTime() {
        UpgradeToOwnerRequest request = UpgradeToOwnerRequest.builder()
                .businessName("My Venue")
                .taxCode("0123456789")
                .businessAddress("HCM City")
                .businessLicenseUrl("http://example.com/license.jpg")
                .identityCardUrl("http://example.com/id.jpg")
                .build();

        User user = User.builder()
                .userId(1)
                .email("customer@example.com")
                .role(customerRole)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.empty());

        Owner savedOwner = Owner.builder()
                .ownerId(10)
                .user(user)
                .businessName(request.getBusinessName())
                .taxCode(request.getTaxCode())
                .businessAddress(request.getBusinessAddress())
                .businessLicenseUrl(request.getBusinessLicenseUrl())
                .identityCardUrl(request.getIdentityCardUrl())
                .approvedStatus(ApprovedStatus.PENDING)
                .build();
        when(ownerRepository.save(any(Owner.class))).thenReturn(savedOwner);

        OwnerDetailResponse result = ownerRegistrationService.upgradeCurrentCustomer(user, request);

        assertNotNull(result);
        assertEquals("My Venue", result.getBusinessName());
        assertEquals("0123456789", result.getTaxCode());
        assertEquals("HCM City", result.getBusinessAddress());
        assertEquals("http://example.com/license.jpg", result.getBusinessLicenseUrl());
        assertEquals("http://example.com/id.jpg", result.getIdentityCardUrl());
        assertEquals(ApprovedStatus.PENDING, result.getApprovedStatus());

        verify(ownerRepository).save(any(Owner.class));
    }

    @Test
    void upgradeCurrentCustomer_Success_ResubmitRejected() {
        UpgradeToOwnerRequest request = UpgradeToOwnerRequest.builder()
                .businessName("New Venue Name")
                .taxCode("9876543210")
                .businessAddress("New Address")
                .businessLicenseUrl("http://example.com/license.jpg")
                .identityCardUrl("http://example.com/id.jpg")
                .build();

        User user = User.builder()
                .userId(1)
                .email("customer@example.com")
                .role(customerRole)
                .build();

        Owner existingRejectedOwner = Owner.builder()
                .ownerId(10)
                .user(user)
                .businessName("Old Name")
                .taxCode("123")
                .businessAddress("Old Address")
                .businessLicenseUrl("http://example.com/old-license.jpg")
                .identityCardUrl("http://example.com/old-id.jpg")
                .approvedStatus(ApprovedStatus.REJECTED)
                .rejectionReason("Inadequate document")
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(existingRejectedOwner));
        when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerDetailResponse result = ownerRegistrationService.upgradeCurrentCustomer(user, request);

        assertNotNull(result);
        assertEquals("New Venue Name", result.getBusinessName());
        assertEquals("9876543210", result.getTaxCode());
        assertEquals("New Address", result.getBusinessAddress());
        assertEquals("http://example.com/license.jpg", result.getBusinessLicenseUrl());
        assertEquals("http://example.com/id.jpg", result.getIdentityCardUrl());
        assertEquals(ApprovedStatus.PENDING, result.getApprovedStatus());
        assertNull(result.getRejectionReason());

        verify(ownerRepository).save(existingRejectedOwner);
    }

    @Test
    void upgradeCurrentCustomer_AlreadyOwner_ThrowsException() {
        UpgradeToOwnerRequest request = UpgradeToOwnerRequest.builder().build();

        User user = User.builder()
                .userId(1)
                .email("customer@example.com")
                .role(ownerRole)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(Owner.builder().approvedStatus(ApprovedStatus.APPROVED).build()));

        AppException ex = assertThrows(AppException.class, () ->
                ownerRegistrationService.upgradeCurrentCustomer(user, request));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
        verify(ownerRepository, never()).save(any());
    }

    @Test
    void upgradeCurrentCustomer_PendingRequest_ThrowsException() {
        UpgradeToOwnerRequest request = UpgradeToOwnerRequest.builder().build();

        User user = User.builder()
                .userId(1)
                .email("customer@example.com")
                .role(customerRole)
                .build();

        Owner existingPendingOwner = Owner.builder()
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(existingPendingOwner));

        AppException ex = assertThrows(AppException.class, () ->
                ownerRegistrationService.upgradeCurrentCustomer(user, request));

        assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.getErrorCode());
        verify(ownerRepository, never()).save(any());
    }

    @Test
    void resubmitOwnerProfile_Success() {
        UpgradeToOwnerRequest request = UpgradeToOwnerRequest.builder()
                .businessName("New Name")
                .taxCode("1234567890")
                .businessAddress("New Addr")
                .build();

        User user = User.builder().userId(1).email("owner@example.com").build();
        Owner existingOwner = Owner.builder()
                .ownerId(5)
                .user(user)
                .approvedStatus(ApprovedStatus.REJECTED)
                .rejectionReason("Bad tax code")
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(existingOwner));
        when(ownerRepository.save(any(Owner.class))).thenAnswer(inv -> inv.getArgument(0));

        OwnerDetailResponse response = ownerRegistrationService.resubmitOwnerProfile(user, request);

        assertNotNull(response);
        assertEquals(ApprovedStatus.PENDING, response.getApprovedStatus());
        assertNull(existingOwner.getRejectionReason());
        assertNull(existingOwner.getApprovedBy());
        assertNull(existingOwner.getApprovedAt());
    }

    @Test
    void getOwnerProfileOfUser_Success() {
        User user = User.builder().userId(1).email("owner@example.com").build();
        Owner owner = Owner.builder()
                .ownerId(5)
                .user(user)
                .businessName("My Business")
                .taxCode("111222333")
                .businessAddress("Da Nang")
                .businessLicenseUrl("http://example.com/license.jpg")
                .identityCardUrl("http://example.com/id.jpg")
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));

        OwnerDetailResponse response = ownerRegistrationService.getOwnerProfileOfUser(user);

        assertNotNull(response);
        assertEquals("My Business", response.getBusinessName());
        assertEquals("111222333", response.getTaxCode());
        assertEquals("Da Nang", response.getBusinessAddress());
        assertEquals("http://example.com/license.jpg", response.getBusinessLicenseUrl());
        assertEquals("http://example.com/id.jpg", response.getIdentityCardUrl());
        assertEquals(ApprovedStatus.APPROVED, response.getApprovedStatus());
    }

    @Test
    void getOwnerProfileOfUser_NotFound() {
        User user = User.builder().userId(1).email("customer@example.com").build();
        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.empty());

        OwnerDetailResponse response = ownerRegistrationService.getOwnerProfileOfUser(user);

        assertNull(response);
    }

    @Test
    void getOwnerRegistrations_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder().userId(1).email("owner@example.com").build();
        Owner owner = Owner.builder()
                .ownerId(5)
                .user(user)
                .businessName("My Business")
                .approvedStatus(ApprovedStatus.PENDING)
                .build();
        Page<Owner> ownerPage = new PageImpl<>(List.of(owner));

        when(ownerRepository.findByApprovedStatus(ApprovedStatus.PENDING, pageable)).thenReturn(ownerPage);

        Page<OwnerDetailResponse> result = ownerRegistrationService.getOwnerRegistrations(ApprovedStatus.PENDING, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("My Business", result.getContent().get(0).getBusinessName());
    }

    @Test
    void approveOrRejectOwner_Approve_CustomerUpgrade_Success() {
        ApproveOwnerRequest request = ApproveOwnerRequest.builder()
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        User user = User.builder()
                .userId(1)
                .email("customer@example.com")
                .role(customerRole)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Owner owner = Owner.builder()
                .ownerId(5)
                .user(user)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        mockSecurityContext("admin@sportvenue.com");
        when(userRepository.findByEmail("admin@sportvenue.com")).thenReturn(Optional.of(User.builder().email("admin@sportvenue.com").build()));

        when(ownerRepository.findById(5)).thenReturn(Optional.of(owner));
        when(roleRepository.findByRoleName("Owner")).thenReturn(Optional.of(ownerRole));
        when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerDetailResponse response = ownerRegistrationService.approveOrRejectOwner(5, request);

        assertNotNull(response);
        assertEquals(ApprovedStatus.APPROVED, response.getApprovedStatus());
        assertNull(response.getRejectionReason());
        assertEquals(ownerRole, user.getRole());
        verify(userRepository).save(user);
        verify(notificationService).createNotification(
                eq(user.getUserId()),
                anyString(),
                anyString(),
                eq(NotificationType.SYSTEM),
                anyString()
        );
    }

    @Test
    void approveOrRejectOwner_Approve_DirectOwnerPending_Success() {
        ApproveOwnerRequest request = ApproveOwnerRequest.builder()
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        User user = User.builder()
                .userId(1)
                .email("owner@example.com")
                .role(ownerRole)
                .accountStatus(AccountStatus.PENDING)
                .build();

        Owner owner = Owner.builder()
                .ownerId(5)
                .user(user)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        mockSecurityContext("admin@sportvenue.com");
        when(userRepository.findByEmail("admin@sportvenue.com")).thenReturn(Optional.of(User.builder().email("admin@sportvenue.com").build()));

        when(ownerRepository.findById(5)).thenReturn(Optional.of(owner));
        when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerDetailResponse response = ownerRegistrationService.approveOrRejectOwner(5, request);

        assertNotNull(response);
        assertEquals(ApprovedStatus.APPROVED, response.getApprovedStatus());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        verify(userRepository).save(user);
        verify(notificationService).createNotification(
                eq(user.getUserId()),
                anyString(),
                anyString(),
                eq(NotificationType.SYSTEM),
                anyString()
        );
    }

    @Test
    void approveOrRejectOwner_Reject_Success() {
        ApproveOwnerRequest request = ApproveOwnerRequest.builder()
                .approvedStatus(ApprovedStatus.REJECTED)
                .rejectionReason("Incomplete documents")
                .build();

        User user = User.builder()
                .userId(1)
                .email("customer@example.com")
                .role(customerRole)
                .build();

        Owner owner = Owner.builder()
                .ownerId(5)
                .user(user)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        mockSecurityContext("admin@sportvenue.com");
        when(userRepository.findByEmail("admin@sportvenue.com")).thenReturn(Optional.of(User.builder().email("admin@sportvenue.com").build()));

        when(ownerRepository.findById(5)).thenReturn(Optional.of(owner));
        when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerDetailResponse response = ownerRegistrationService.approveOrRejectOwner(5, request);

        assertNotNull(response);
        assertEquals(ApprovedStatus.REJECTED, response.getApprovedStatus());
        assertEquals("Incomplete documents", response.getRejectionReason());
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService).createNotification(
                eq(user.getUserId()),
                anyString(),
                anyString(),
                eq(NotificationType.SYSTEM),
                anyString()
        );
    }

    @Test
    void approveOrRejectOwner_OwnerNotFound_ThrowsException() {
        ApproveOwnerRequest request = ApproveOwnerRequest.builder()
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        when(ownerRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                ownerRegistrationService.approveOrRejectOwner(999, request));
    }

    @Test
    void approveOrRejectOwner_AlreadyProcessed_ThrowsException() {
        ApproveOwnerRequest request = ApproveOwnerRequest.builder()
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        Owner owner = Owner.builder()
                .ownerId(5)
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        when(ownerRepository.findById(5)).thenReturn(Optional.of(owner));

        assertThrows(BadRequestException.class, () ->
                ownerRegistrationService.approveOrRejectOwner(5, request));
    }

    @Test
    void approveOrRejectOwner_Reject_MissingReason_ThrowsException() {
        ApproveOwnerRequest request = ApproveOwnerRequest.builder()
                .approvedStatus(ApprovedStatus.REJECTED)
                .rejectionReason("")
                .build();

        Owner owner = Owner.builder()
                .ownerId(5)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        mockSecurityContext("admin@sportvenue.com");
        when(userRepository.findByEmail("admin@sportvenue.com")).thenReturn(Optional.of(User.builder().email("admin@sportvenue.com").build()));

        when(ownerRepository.findById(5)).thenReturn(Optional.of(owner));

        assertThrows(BadRequestException.class, () ->
                ownerRegistrationService.approveOrRejectOwner(5, request));
    }
}
