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
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.RoleRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.OwnerRegistrationService;
import com.sportvenue.service.OtpService;
import com.sportvenue.entity.enums.UserRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .map(owner -> OwnerDetailResponse.builder()
                        .ownerId(owner.getOwnerId())
                        .userId(currentUser.getUserId())
                        .fullName(currentUser.getFullName())
                        .email(currentUser.getEmail())
                        .phoneNumber(currentUser.getPhoneNumber())
                        .businessName(owner.getBusinessName())
                        .taxCode(owner.getTaxCode())
                        .businessAddress(owner.getBusinessAddress())
                        .approvedStatus(owner.getApprovedStatus())
                        .rejectionReason(owner.getRejectionReason())
                        .createdAt(owner.getCreatedAt())
                        .build())
                .orElse(null);
    }
}
