package com.sportvenue.security;

import com.sportvenue.entity.Owner;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Aspect kiểm tra trạng thái phê duyệt của Owner trước khi thực thi Method/Controller được đánh dấu.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovedOwnerAspect {

    private final OwnerRepository ownerRepository;

    @Before("@within(com.sportvenue.security.RequireApprovedOwner) || @annotation(com.sportvenue.security.RequireApprovedOwner)")
    public void checkOwnerApproved() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Yêu cầu xác thực thông tin tài khoản.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new AccessDeniedException("Thông tin tài khoản không hợp lệ.");
        }

        Integer userId = userPrincipal.getUser().getUserId();
        Optional<Owner> ownerOpt = ownerRepository.findByUserUserId(userId);

        if (ownerOpt.isEmpty()) {
            log.warn("User ID {} triggers @RequireApprovedOwner check but has no owner profile.", userId);
            throw new AccessDeniedException("Hồ sơ đối tác chủ sân không tồn tại.");
        }

        ApprovedStatus status = ownerOpt.get().getApprovedStatus();
        if (status != ApprovedStatus.APPROVED) {
            log.warn("Access denied: Owner profile for user ID {} is in state: {}", userId, status);
            throw new AccessDeniedException("Tài khoản chủ sân của bạn chưa được Admin phê duyệt hoặc đã bị từ chối.");
        }
    }
}
