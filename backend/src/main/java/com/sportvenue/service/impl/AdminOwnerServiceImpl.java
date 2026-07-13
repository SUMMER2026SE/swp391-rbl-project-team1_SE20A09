package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.AdminAccountLockService;
import com.sportvenue.service.AdminOwnerService;
import com.sportvenue.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOwnerServiceImpl implements AdminOwnerService {

    private final OwnerRepository ownerRepository;
    private final StadiumRepository stadiumRepository;
    private final EmailService emailService;
    private final AdminAccountLockService adminAccountLockService;

    @Override
    public PageResponse<AdminOwnerResponse> getOwners(String search, String accountStatusStr, String approvedStatusStr,
            int page, int size, String sortBy, String sortDir) {
        log.info(
                "Fetching owners list with search: {}, accountStatus: {}, approvedStatus: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                search, accountStatusStr, approvedStatusStr, page, size, sortBy, sortDir);

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortColumn = "createdAt";
        if ("fullName".equals(sortBy)) {
            sortColumn = "user.firstName";
        } else if ("email".equals(sortBy)) {
            sortColumn = "user.email";
        } else if ("phoneNumber".equals(sortBy)) {
            sortColumn = "user.phoneNumber";
        } else if ("businessName".equals(sortBy)) {
            sortColumn = "businessName";
        } else if ("createdAt".equals(sortBy)) {
            sortColumn = "createdAt";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortColumn));

        AccountStatus accStatus = null;
        if (accountStatusStr != null && !accountStatusStr.isBlank()) {
            try {
                accStatus = AccountStatus.valueOf(accountStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid account status: {}", accountStatusStr);
            }
        }

        ApprovedStatus appStatus = null;
        if (approvedStatusStr != null && !approvedStatusStr.isBlank()) {
            try {
                appStatus = ApprovedStatus.valueOf(approvedStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid approved status: {}", approvedStatusStr);
            }
        }

        String searchPattern = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<AdminOwnerResponse> ownerPage = ownerRepository.findOwnersForAdmin(searchPattern, accStatus, appStatus,
                pageable);

        PageResponse<AdminOwnerResponse> response = new PageResponse<>();
        response.setContent(ownerPage.getContent());
        response.setPageNumber(ownerPage.getNumber());
        response.setPageSize(ownerPage.getSize());
        response.setTotalElements(ownerPage.getTotalElements());
        response.setTotalPages(ownerPage.getTotalPages());
        response.setLast(ownerPage.isLast());

        return response;
    }

    @Override
    @Transactional
    public void lockUnlockOwner(Integer ownerId, boolean isEnabled, String reason, Integer currentAdminId) {
        log.info("Request to {} account for ownerId: {}", isEnabled ? "unlock" : "lock", ownerId);
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new AppException(ErrorCode.OWNER_PROFILE_NOT_APPROVED);
        }

        User user = owner.getUser();
        adminAccountLockService.applyLockState(user, isEnabled, currentAdminId, reason);

        List<Stadium> stadiums = stadiumRepository.findByOwnerOwnerId(ownerId);
        List<Stadium> toUpdate;
        if (!isEnabled) {
            toUpdate = stadiums.stream()
                    .filter(s -> s.getStadiumStatus() == StadiumStatus.AVAILABLE)
                    .peek(s -> s.setStadiumStatus(StadiumStatus.MAINTENANCE))
                    .toList();
        } else {
            toUpdate = stadiums.stream()
                    .filter(s -> s.getStadiumStatus() == StadiumStatus.MAINTENANCE)
                    .filter(s -> !Boolean.TRUE.equals(s.getAdminSuspended()))
                    .peek(s -> s.setStadiumStatus(StadiumStatus.AVAILABLE))
                    .toList();
        }
        if (!toUpdate.isEmpty()) {
            stadiumRepository.saveAll(toUpdate);
        }

        try {
            emailService.sendAccountStatusNotification(user.getEmail(), owner.getBusinessName(), isEnabled, reason);
        } catch (Exception e) {
            log.error("Failed to send notification email", e);
        }
    }
}
