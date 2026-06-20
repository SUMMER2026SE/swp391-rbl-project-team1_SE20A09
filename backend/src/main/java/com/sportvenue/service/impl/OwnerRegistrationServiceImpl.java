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
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.OwnerRegistrationService;
import com.sportvenue.service.OtpService;
import com.sportvenue.entity.enums.UserRank;
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

    @Override
    @Transactional
    public MessageResponse registerNewOwner(RegisterOwnerRequest request) {
        log.info("Processing sign up request for new owner email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE); // Ném lỗi nếu trùng email
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        Role ownerRole = roleRepository.findByRoleName("Owner")
                .orElseThrow(() -> new RuntimeException("Role 'Owner' not found in database"));

        // Tách họ và tên (Tránh BUG hardcode lastName rỗng)
        String[] nameParts = request.getFullName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : firstName;

        // 1. Tạo tài khoản User mới có role = Owner, trạng thái ban đầu là PENDING (chờ duyệt)
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

        // 2. Tạo hồ sơ kinh doanh Owner trạng thái PENDING
        Owner owner = Owner.builder()
                .user(savedUser)
                .businessName(request.getBusinessName())
                .taxCode(request.getTaxCode())
                .businessAddress(request.getBusinessAddress())
                .approvedStatus(ApprovedStatus.PENDING)
                .build();
        ownerRepository.save(owner);

        // 3. Gửi OTP xác thực tài khoản qua email để tránh spam
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
                // Cho phép cập nhật lại thông tin và gửi duyệt lại
                existingOwner.setBusinessName(request.getBusinessName());
                existingOwner.setTaxCode(request.getTaxCode());
                existingOwner.setBusinessAddress(request.getBusinessAddress());
                existingOwner.setApprovedStatus(ApprovedStatus.PENDING);
                existingOwner.setRejectionReason(null);
                owner = existingOwner;
            } else {
                throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
            }
        } else {
            // Tạo mới hồ sơ Owner PENDING
            owner = Owner.builder()
                    .user(currentUser)
                    .businessName(request.getBusinessName())
                    .taxCode(request.getTaxCode())
                    .businessAddress(request.getBusinessAddress())
                    .approvedStatus(ApprovedStatus.PENDING)
                    .build();
        }

        Owner savedOwner = ownerRepository.save(owner);

        return OwnerDetailResponse.builder()
                .ownerId(savedOwner.getOwnerId())
                .userId(currentUser.getUserId())
                .fullName(currentUser.getFullName())
                .email(currentUser.getEmail())
                .phoneNumber(currentUser.getPhoneNumber())
                .businessName(savedOwner.getBusinessName())
                .taxCode(savedOwner.getTaxCode())
                .businessAddress(savedOwner.getBusinessAddress())
                .approvedStatus(savedOwner.getApprovedStatus())
                .createdAt(savedOwner.getCreatedAt())
                .build();
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

        User user = owner.getUser();

        if (request.getApprovedStatus() == ApprovedStatus.APPROVED) {
            // 1. Cập nhật hồ sơ chủ sân sang APPROVED
            owner.setApprovedStatus(ApprovedStatus.APPROVED);
            owner.setRejectionReason(null);

            // 2. Nâng cấp vai trò của User lên Owner nếu hiện tại là Customer
            if ("Customer".equals(user.getRole().getRoleName())) {
                Role ownerRole = roleRepository.findByRoleName("Owner")
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Role Owner trong DB"));
                user.setRole(ownerRole);
            }

            // 3. Kích hoạt tài khoản User nếu đang chờ duyệt
            if (user.getAccountStatus() == AccountStatus.PENDING) {
                user.setAccountStatus(AccountStatus.ACTIVE);
            }

            userRepository.save(user);
            log.info("Owner registration APPROVED for email: {}. Role upgraded to Owner.", user.getEmail());

        } else if (request.getApprovedStatus() == ApprovedStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Lý do từ chối là bắt buộc");
            }

            // 1. Từ chối hồ sơ
            owner.setApprovedStatus(ApprovedStatus.REJECTED);
            owner.setRejectionReason(request.getRejectionReason().trim());

            log.info("Owner registration REJECTED for email: {}. Reason: {}", user.getEmail(), request.getRejectionReason());
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
                .createdAt(owner.getCreatedAt())
                .build();
    }
}
