package com.sportvenue.security;

import com.sportvenue.entity.Owner;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.repository.OwnerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Component hỗ trợ kiểm tra điều kiện phê duyệt (approved_status) của Owner trong Spring Security.
 * Cho phép dùng trực tiếp trong SpEL: @PreAuthorize("hasRole('Owner') and @securityEvaluator.isApprovedOwner()")
 */
@Component("securityEvaluator")
@RequiredArgsConstructor
@Slf4j
public class SecurityEvaluator {

    private final OwnerRepository ownerRepository;

    /**
     * Kiểm tra người dùng hiện tại (phải là Owner) đã được Admin phê duyệt hồ sơ chưa.
     *
     * @return true nếu người dùng là Owner và trạng thái hồ sơ là APPROVED, ngược lại false
     */
    public boolean isApprovedOwner() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (attributes != null) {
            request = attributes.getRequest();
            Boolean cached = (Boolean) request.getAttribute("IS_APPROVED_OWNER_KEY");
            if (cached != null) {
                return cached;
            }
        }

        boolean result = evaluateIsApprovedOwner();

        if (request != null) {
            request.setAttribute("IS_APPROVED_OWNER_KEY", result);
        }

        return result;
    }

    private boolean evaluateIsApprovedOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return false;
        }

        Integer userId = userPrincipal.getUser().getUserId();
        Optional<Owner> ownerOpt = ownerRepository.findByUserUserId(userId);

        if (ownerOpt.isEmpty()) {
            log.warn("User ID {} has role Owner but no owner profile found in owners table", userId);
            return false;
        }

        ApprovedStatus status = ownerOpt.get().getApprovedStatus();
        if (status == ApprovedStatus.APPROVED) {
            return true;
        }

        log.warn("Denied access to owner API: Owner profile for user ID {} is in state: {}", userId, status);
        return false;
    }
}
