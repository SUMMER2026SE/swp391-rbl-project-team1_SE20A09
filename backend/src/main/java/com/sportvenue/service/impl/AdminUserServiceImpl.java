package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AccountStatusService;
import com.sportvenue.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation cho AdminUserService.
 * UC-ADM-02: View List Customer — lấy danh sách khách hàng có phân trang.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AccountStatusService accountStatusService;

    private static final String ROLE_CUSTOMER = "Customer";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminCustomerResponse> getCustomers(String search, AccountStatus accountStatus, Pageable pageable) {
        log.info("Admin fetching customer list — search='{}', status={}, page={}, size={}",
                search, accountStatus, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findByRoleWithFilters(
                ROLE_CUSTOMER,
                search,
                accountStatus,
                pageable
        );

        return PageResponse.of(userPage, userPage.getContent().stream()
                .map(this::toAdminCustomerResponse)
                .toList());
    }

    /**
     * Map từ User entity sang AdminCustomerResponse DTO.
     * Không expose password_hash, role_id hay các field nhạy cảm khác.
     */
    private AdminCustomerResponse toAdminCustomerResponse(User user) {
        return AdminCustomerResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus())
                .userRank(user.getUserRank())
                .userPoint(user.getUserPoint())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void lockUnlockCustomer(Integer id, Boolean enabled, Integer currentAdminId) {
        log.info("Admin {} requesting to lock/unlock customer id={}, enabled={}", currentAdminId, id, enabled);

        if (id.equals(currentAdminId)) {
            throw new com.sportvenue.exception.BadRequestException("Bạn không thể tự khóa tài khoản của chính mình.");
        }

        User customer = userRepository.findById(id)
                .orElseThrow(() -> new com.sportvenue.exception.AppException(com.sportvenue.exception.ErrorCode.USER_NOT_FOUND));

        if (!ROLE_CUSTOMER.equalsIgnoreCase(customer.getRole().getRoleName())) {
            throw new com.sportvenue.exception.ResourceNotFoundException("Người dùng không phải là khách hàng (Customer).");
        }

        AccountStatus newStatus = accountStatusService.applyAdminLockState(customer, enabled);
        userRepository.save(customer);
        
        log.info("Successfully updated account status to {} for customer id={}", newStatus, id);
    }
}
