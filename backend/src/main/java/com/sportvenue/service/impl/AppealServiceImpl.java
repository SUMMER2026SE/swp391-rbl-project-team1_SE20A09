package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateAppealRequest;
import com.sportvenue.dto.request.ReviewAppealRequest;
import com.sportvenue.dto.response.AppealResponse;
import com.sportvenue.entity.Appeal;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.AppealStatus;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.AppealRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AdminOwnerService;
import com.sportvenue.service.AppealService;
import com.sportvenue.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final AdminOwnerService adminOwnerService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AppealResponse createAppeal(CreateAppealRequest request, UserPrincipal userPrincipal) {
        User user = getAuthenticatedUser(userPrincipal);
        if (user.getAccountStatus() != AccountStatus.BLOCKED) {
            throw new BadRequestException("Chỉ tài khoản đang bị khóa mới được gửi kháng cáo.");
        }
        if (appealRepository.existsByUserUserIdAndStatus(user.getUserId(), AppealStatus.PENDING)) {
            throw new BadRequestException("Bạn đã có một kháng cáo đang chờ xử lý.");
        }

        Appeal appeal = Appeal.builder()
                .user(user)
                .relatedLockReason(user.getLockReason())
                .appealText(request.getAppealText().trim())
                .evidenceUrls(normalizeEvidenceUrls(request.getEvidenceUrls()))
                .status(AppealStatus.PENDING)
                .build();

        Appeal saved = appealRepository.save(appeal);
        notifyAdmins(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppealResponse getMyLatestAppeal(UserPrincipal userPrincipal) {
        User user = getAuthenticatedUser(userPrincipal);
        return appealRepository.findTopByUserUserIdOrderByCreatedAtDesc(user.getUserId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppealResponse> getAppeals(AppealStatus status, Pageable pageable) {
        AppealStatus filterStatus = status == null ? AppealStatus.PENDING : status;
        return appealRepository.findByStatus(filterStatus, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public AppealResponse reviewAppeal(Integer appealId, ReviewAppealRequest request, UserPrincipal adminPrincipal) {
        User admin = getAuthenticatedUser(adminPrincipal);
        if (request.getStatus() == AppealStatus.PENDING) {
            throw new BadRequestException("Admin chỉ có thể duyệt hoặc từ chối kháng cáo.");
        }

        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kháng cáo."));
        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BadRequestException("Kháng cáo này đã được xử lý.");
        }

        appeal.setStatus(request.getStatus());
        appeal.setReviewedBy(admin);
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setAdminNote(trimToNull(request.getAdminNote()));

        if (request.getStatus() == AppealStatus.APPROVED) {
            unlockAppealedUser(appeal.getUser());
        }

        Appeal saved = appealRepository.save(appeal);
        notifyUserAboutDecision(saved);
        return toResponse(saved);
    }

    private User getAuthenticatedUser(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new BadRequestException("Bạn cần đăng nhập để thực hiện thao tác này.");
        }
        return userRepository.findById(userPrincipal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));
    }

    private void unlockAppealedUser(User lockedUser) {
        ownerRepository.findByUserUserId(lockedUser.getUserId())
                .map(Owner::getOwnerId)
                .ifPresentOrElse(
                        ownerId -> adminOwnerService.lockUnlockOwner(ownerId, true, null),
                        () -> {
                            lockedUser.setAccountStatus(AccountStatus.ACTIVE);
                            lockedUser.setLockReason(null);
                            userRepository.save(lockedUser);
                        });
    }

    private List<String> normalizeEvidenceUrls(List<String> evidenceUrls) {
        if (evidenceUrls == null) {
            return Collections.emptyList();
        }
        return evidenceUrls.stream()
                .map(url -> url == null ? "" : url.trim())
                .filter(url -> !url.isBlank())
                .distinct()
                .toList();
    }

    private void notifyAdmins(Appeal appeal) {
        String resourceId = "APPEAL-" + appeal.getAppealId();
        userRepository.findAllAdmins().forEach(admin ->
                notificationService.createNotification(
                        admin.getUserId(),
                        "Kháng cáo tài khoản mới",
                        appeal.getUser().getFullName() + " đã gửi kháng cáo mở khóa tài khoản.",
                        NotificationType.APPEAL,
                        resourceId));
    }

    private void notifyUserAboutDecision(Appeal appeal) {
        boolean approved = appeal.getStatus() == AppealStatus.APPROVED;
        notificationService.createNotification(
                appeal.getUser().getUserId(),
                approved ? "Kháng cáo đã được chấp nhận" : "Kháng cáo đã bị từ chối",
                approved
                        ? "Tài khoản của bạn đã được mở khóa."
                        : "Admin đã từ chối kháng cáo của bạn.",
                NotificationType.APPEAL,
                "APPEAL-" + appeal.getAppealId());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private AppealResponse toResponse(Appeal appeal) {
        User reviewedBy = appeal.getReviewedBy();
        return AppealResponse.builder()
                .appealId(appeal.getAppealId())
                .userId(appeal.getUser().getUserId())
                .userEmail(appeal.getUser().getEmail())
                .userFullName(appeal.getUser().getFullName())
                .relatedLockReason(appeal.getRelatedLockReason())
                .appealText(appeal.getAppealText())
                .evidenceUrls(List.copyOf(appeal.getEvidenceUrls()))
                .status(appeal.getStatus())
                .reviewedByAdminId(reviewedBy != null ? reviewedBy.getUserId() : null)
                .reviewedByAdminEmail(reviewedBy != null ? reviewedBy.getEmail() : null)
                .reviewedAt(appeal.getReviewedAt())
                .adminNote(appeal.getAdminNote())
                .createdAt(appeal.getCreatedAt())
                .build();
    }
}
