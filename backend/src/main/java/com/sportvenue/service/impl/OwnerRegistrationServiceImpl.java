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
import com.sportvenue.service.OtpService;
import com.sportvenue.service.NotificationService;
import com.sportvenue.service.OwnerRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerRegistrationServiceImpl implements OwnerRegistrationService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public MessageResponse registerNewOwner(RegisterOwnerRequest request) {
        log.info("Processing sign up request for new owner email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        if (userRepository.existsByPhoneNumber(request.getPhone())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        Role ownerRole = roleRepository.findByRoleName("Owner")
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

        String[] nameParts = request.getFullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(request.getEmail())
                .phoneNumber(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(ownerRole)
                .accountStatus(AccountStatus.PENDING)
                .isVerified(false)
                .userRank(UserRank.BRONZE)
                .userPoint(0)
                .build();
        User savedUser = userRepository.save(user);

        Owner owner = Owner.builder()
                .user(savedUser)
                .businessName(request.getBusinessName())
                .taxCode(request.getTaxCode())
                .businessAddress(request.getBusinessAddress())
                .businessLicenseUrl(request.getBusinessLicenseUrl())
                .identityCardUrl(request.getIdentityCardUrl())
                .approvedStatus(ApprovedStatus.PENDING)
                .build();
        ownerRepository.save(owner);

        otpService.createAndSendOtp(savedUser);

        return new MessageResponse("Đăng ký thành công. Vui lòng kiểm tra email để nhận mã xác thực OTP.");
    }

    @Override
    @Transactional
    public OwnerDetailResponse upgradeCurrentCustomer(User currentUser, UpgradeToOwnerRequest request) {
        log.info("Processing upgrade registration for Customer: {}", currentUser.getEmail());

        java.util.Optional<Owner> existingOwnerOpt = ownerRepository.findByUserUserId(currentUser.getUserId());
        Owner owner;

        if (existingOwnerOpt.isPresent()) {
            Owner existingOwner = existingOwnerOpt.get();
            if (existingOwner.getApprovedStatus() == ApprovedStatus.REJECTED) {
                existingOwner.setBusinessName(request.getBusinessName());
                existingOwner.setTaxCode(request.getTaxCode());
                existingOwner.setBusinessAddress(request.getBusinessAddress());
                existingOwner.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
                existingOwner.setIdentityCardUrl(request.getIdentityCardUrl());
                existingOwner.setApprovedStatus(ApprovedStatus.PENDING);
                existingOwner.setRejectionReason(null);
                existingOwner.setApprovedBy(null);
                existingOwner.setApprovedAt(null);
                owner = existingOwner;
            } else {
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
            }
        } else {
            owner = Owner.builder()
                    .user(currentUser)
                    .businessName(request.getBusinessName())
                    .taxCode(request.getTaxCode())
                    .businessAddress(request.getBusinessAddress())
                    .businessLicenseUrl(request.getBusinessLicenseUrl())
                    .identityCardUrl(request.getIdentityCardUrl())
                    .approvedStatus(ApprovedStatus.PENDING)
                    .build();
        }

        Owner savedOwner = ownerRepository.save(owner);
        return mapToOwnerDetailResponse(savedOwner);
    }

    @Override
    @Transactional
    public OwnerDetailResponse resubmitOwnerProfile(User currentUser, UpgradeToOwnerRequest request) {
        log.info("Processing resubmit profile for rejected Owner: {}", currentUser.getEmail());

        Owner existingOwner = ownerRepository.findByUserUserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));

        if (existingOwner.getApprovedStatus() != ApprovedStatus.REJECTED) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        existingOwner.setBusinessName(request.getBusinessName());
        existingOwner.setTaxCode(request.getTaxCode());
        existingOwner.setBusinessAddress(request.getBusinessAddress());
        existingOwner.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
        existingOwner.setIdentityCardUrl(request.getIdentityCardUrl());
        existingOwner.setApprovedStatus(ApprovedStatus.PENDING);
        existingOwner.setRejectionReason(null);
        existingOwner.setApprovedBy(null);
        existingOwner.setApprovedAt(null);
        Owner savedOwner = ownerRepository.save(existingOwner);

        log.info("Owner {} resubmitted profile successfully, status reset to PENDING", currentUser.getEmail());

        return mapToOwnerDetailResponse(savedOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerDetailResponse getOwnerProfileOfUser(User currentUser) {
        return ownerRepository.findByUserUserId(currentUser.getUserId())
                .map(this::mapToOwnerDetailResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OwnerDetailResponse> getOwnerRegistrations(ApprovedStatus status, Pageable pageable) {
        log.info("Admin fetching owner registrations with status: {}", status);
        Page<Owner> owners = ownerRepository.findByApprovedStatus(status, pageable);
        return owners.map(this::mapToOwnerDetailResponse);
    }

    @Override
    @Transactional
    public OwnerDetailResponse approveOrRejectOwner(Integer ownerId, ApproveOwnerRequest request) {
        log.info("Admin is processing ownerId: {} with status: {}", ownerId, request.getApprovedStatus());

        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ đối tác với ID: " + ownerId));

        if (owner.getApprovedStatus() != ApprovedStatus.PENDING) {
            throw new BadRequestException("Hồ sơ này đã được xử lý và không thể thay đổi trạng thái.");
        }
        if (request.getApprovedStatus() == ApprovedStatus.PENDING) {
            throw new BadRequestException("Không thể set trạng thái PENDING từ hành động này");
        }

        User user = owner.getUser();

        String adminEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin Admin"));

        owner.setApprovedBy(admin);
        owner.setApprovedAt(java.time.LocalDateTime.now());

        if (request.getApprovedStatus() == ApprovedStatus.APPROVED) {
            owner.setApprovedStatus(ApprovedStatus.APPROVED);
            owner.setRejectionReason(null);

            if ("Customer".equals(user.getRole().getRoleName())) {
                Role ownerRole = roleRepository.findByRoleName("Owner")
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Role Owner trong DB"));
                user.setRole(ownerRole);
            }

            if (user.getAccountStatus() == AccountStatus.PENDING) {
                user.setAccountStatus(AccountStatus.ACTIVE);
            }
            user.setIsVerified(true);

            userRepository.save(user);
            log.info("Owner registration APPROVED for email: {}. Role upgraded to Owner.", user.getEmail());

            notificationService.createNotification(
                    user.getUserId(),
                    "Hồ sơ đối tác đã được duyệt",
                    "Chúc mừng! Hồ sơ đối tác cho doanh nghiệp '" + owner.getBusinessName() + "' đã được Admin phê duyệt thành công. Bạn hiện có quyền của Chủ sân.",
                    NotificationType.SYSTEM,
                    owner.getOwnerId().toString()
            );

        } else if (request.getApprovedStatus() == ApprovedStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Lý do từ chối là bắt buộc");
            }

            owner.setApprovedStatus(ApprovedStatus.REJECTED);
            owner.setRejectionReason(request.getRejectionReason().trim());

            log.info("Owner registration REJECTED for email: {}. Reason: {}", user.getEmail(), request.getRejectionReason());

            notificationService.createNotification(
                    user.getUserId(),
                    "Hồ sơ đối tác bị từ chối",
                    "Hồ sơ đối tác cho doanh nghiệp '" + owner.getBusinessName() + "' đã bị từ chối. Lý do: " + request.getRejectionReason(),
                    NotificationType.SYSTEM,
                    owner.getOwnerId().toString()
            );
        }

        Owner savedOwner = ownerRepository.save(owner);
        return mapToOwnerDetailResponse(savedOwner);
    }

    private OwnerDetailResponse mapToOwnerDetailResponse(Owner owner) {
        return OwnerDetailResponse.builder()
                .ownerId(owner.getOwnerId())
                .userId(owner.getUser().getUserId())
                .fullName(owner.getUser().getFullName())
                .email(owner.getUser().getEmail())
                .phoneNumber(owner.getUser().getPhoneNumber())
                .businessName(owner.getBusinessName())
                .taxCode(owner.getTaxCode())
                .businessAddress(owner.getBusinessAddress())
                .approvedStatus(owner.getApprovedStatus())
                .rejectionReason(owner.getRejectionReason())
                .businessLicenseUrl(owner.getBusinessLicenseUrl())
                .identityCardUrl(owner.getIdentityCardUrl())
                .createdAt(owner.getCreatedAt())
                .approvedByEmail(owner.getApprovedBy() != null ? owner.getApprovedBy().getEmail() : null)
                .approvedAt(owner.getApprovedAt())
                .build();
    }
}
