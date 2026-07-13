package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AccountStatusHistoryService;
import com.sportvenue.service.AccountStatusService;
import com.sportvenue.service.AdminUserService;
import com.sportvenue.service.CustomerNotificationService;
import com.sportvenue.service.NotificationService;
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
    private final AccountStatusHistoryService accountStatusHistoryService;
    private final AccountStatusService accountStatusService;
    private final NotificationService notificationService;
    private final CustomerNotificationService customerNotificationService;

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
    public void lockUnlockCustomer(Integer id, Boolean enabled, Integer currentAdminId, String reason) {
        log.info("Admin {} requesting to lock/unlock customer id={}, enabled={}", currentAdminId, id, enabled);

        if (id.equals(currentAdminId)) {
            throw new com.sportvenue.exception.BadRequestException("Bạn không thể tự khóa tài khoản của chính mình.");
        }

        User customer = userRepository.findById(id)
                .orElseThrow(() -> new com.sportvenue.exception.AppException(com.sportvenue.exception.ErrorCode.USER_NOT_FOUND));

        if (!ROLE_CUSTOMER.equalsIgnoreCase(customer.getRole().getRoleName())) {
            throw new com.sportvenue.exception.ResourceNotFoundException("Người dùng không phải là khách hàng (Customer).");
        }

        AccountStatus previousStatus = customer.getAccountStatus();
        AccountStatus newStatus = accountStatusService.applyAdminLockState(customer, enabled);
        customer.setLockReason(Boolean.TRUE.equals(enabled) ? null : reason);
        accountStatusHistoryService.recordStatusChange(customer, currentAdminId, previousStatus, newStatus, reason);
        userRepository.save(customer);
        if (newStatus == AccountStatus.BLOCKED) {
            notificationService.createNotification(
                    customer.getUserId(),
                    "Tài khoản đã bị khóa",
                    "Tài khoản của bạn đã bị Admin khóa. Lý do: "
                            + (reason == null || reason.isBlank() ? "Không có ghi chú." : reason)
                            + " Bạn có thể gửi kháng cáo trong hệ thống.",
                    NotificationType.ACCOUNT_LOCK,
                    "USER-" + customer.getUserId());
            try {
                customerNotificationService.notifyAccountLocked(customer.getUserId(), reason);
            } catch (Exception ex) {
                log.warn("Failed to emit account locked notification for customer {}", customer.getUserId(), ex);
            }
        } else if (newStatus == AccountStatus.ACTIVE && previousStatus == AccountStatus.BLOCKED) {
            try {
                customerNotificationService.notifyAccountUnlocked(customer.getUserId());
            } catch (Exception ex) {
                log.warn("Failed to emit account unlocked notification for customer {}", customer.getUserId(), ex);
            }
        }
        
        log.info("Successfully updated account status to {} for customer id={}", newStatus, id);
    }
}
