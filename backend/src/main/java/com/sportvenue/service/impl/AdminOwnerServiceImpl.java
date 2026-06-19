package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.service.AdminOwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOwnerServiceImpl implements AdminOwnerService {

    private final OwnerRepository ownerRepository;

    @Override
    public PageResponse<AdminOwnerResponse> getOwners(String search, String accountStatusStr, String approvedStatusStr, int page, int size, String sortBy, String sortDir) {
        log.info("Fetching owners list with search: {}, accountStatus: {}, approvedStatus: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                search, accountStatusStr, approvedStatusStr, page, size, sortBy, sortDir);

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Handle sorting column mapping
        String sortColumn = "createdAt"; // default
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

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortColumn));

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

        Page<AdminOwnerResponse> ownerPage = ownerRepository.findOwnersForAdmin(searchPattern, accStatus, appStatus, pageable);

        PageResponse<AdminOwnerResponse> response = new PageResponse<>();
        response.setContent(ownerPage.getContent());
        response.setPageNumber(ownerPage.getNumber() + 1);
        response.setPageSize(ownerPage.getSize());
        response.setTotalElements(ownerPage.getTotalElements());
        response.setTotalPages(ownerPage.getTotalPages());
        response.setLast(ownerPage.isLast());

        return response;
    }
}
