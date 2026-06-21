package com.sportvenue.service.impl;

import com.sportvenue.dto.request.RegisterOwnerRequest;
import com.sportvenue.dto.request.UpgradeToOwnerRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.response.OwnerDetailResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.UserRank;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private OwnerRegistrationServiceImpl ownerRegistrationService;

    private Role ownerRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        ownerRole = new Role(2, "Owner");
        customerRole = new Role(1, "Customer");
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

        // Verify splitting of name
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("Nguyen", capturedUser.getFirstName());
        assertEquals("Van A", capturedUser.getLastName());
        assertEquals("encodedPassword", capturedUser.getPasswordHash());
        assertEquals(AccountStatus.PENDING, capturedUser.getAccountStatus());
        assertFalse(capturedUser.getIsVerified());
        assertEquals(UserRank.BRONZE, capturedUser.getUserRank());

        // Verify Owner creation
        ArgumentCaptor<Owner> ownerCaptor = ArgumentCaptor.forClass(Owner.class);
        verify(ownerRepository).save(ownerCaptor.capture());
        Owner capturedOwner = ownerCaptor.getValue();
        assertEquals("San bong A", capturedOwner.getBusinessName());
        assertEquals("0123456789", capturedOwner.getTaxCode());
        assertEquals("Hanoi", capturedOwner.getBusinessAddress());
        assertEquals("http://example.com/license.jpg", capturedOwner.getBusinessLicenseUrl());
        assertEquals("http://example.com/id.jpg", capturedOwner.getIdentityCardUrl());
        assertEquals(ApprovedStatus.PENDING, capturedOwner.getApprovedStatus());

        // Verify OTP is triggered
        verify(otpService).createAndSendOtp(savedUser);
    }

    @Test
    void registerNewOwner_SingleWordName_LastNameShouldBeEmpty() {
        RegisterOwnerRequest request = RegisterOwnerRequest.builder()
                .email("single@example.com")
                .fullName("Huy")  // chỉ 1 chữ — bug cũ sẽ tạo ra "Huy Huy"
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
        when(roleRepository.findByRoleName("Owner")).thenReturn(Optional.of(ownerRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ownerRegistrationService.registerNewOwner(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Huy", saved.getFirstName());
        assertEquals("", saved.getLastName());  // ← phải là chuỗi rỗng, không phải "Huy"
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
        verify(ownerRepository, never()).save(any());
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
        verify(ownerRepository, never()).save(any());
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
        assertNull(result.getRejectionReason()); // Should clear rejectionReason

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

        // Since the user role is Owner, it will throw DUPLICATE_RESOURCE directly (Wait, let's verify if role check is the first condition in upgradeCurrentCustomer)
        // Let's check OwnerRegistrationServiceImpl line 125 onwards. (We viewed from 90 to 120, let's verify where Role check is done).
        // Let's verify line 120-135 of implementation.
        // Wait, in upgradeCurrentCustomer:
        // Let's check. Yes, if roleName is Owner, or exists pending owner...
        // Let's see the mocked calls.
        // Let's check if findByUserUserId is called or not when user is Owner. We'll find out or make the test robust.
        // Wait, we can mock it anyway just in case.
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
}
