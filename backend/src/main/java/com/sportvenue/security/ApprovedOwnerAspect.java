package com.sportvenue.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Aspect kiểm tra trạng thái phê duyệt của Owner trước khi thực thi Method/Controller được đánh dấu.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovedOwnerAspect {

    private final SecurityEvaluator securityEvaluator;

    @Before("@within(com.sportvenue.security.RequireApprovedOwner) || @annotation(com.sportvenue.security.RequireApprovedOwner)")
    public void checkOwnerApproved() {
        if (!securityEvaluator.isApprovedOwner()) {
            throw new AccessDeniedException("Tài khoản chủ sân của bạn chưa được Admin phê duyệt hoặc đã bị từ chối.");
        }
    }
}
