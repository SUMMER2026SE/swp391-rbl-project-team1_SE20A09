package com.sportvenue.service;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.PageResponse;

public interface AdminOwnerService {
    PageResponse<AdminOwnerResponse> getOwners(String search, String accountStatus, String approvedStatus, int page, int size, String sortBy, String sortDir);

    default void lockUnlockOwner(Integer ownerId, boolean isEnabled, String reason) {
        lockUnlockOwner(ownerId, isEnabled, reason, null);
    }

    void lockUnlockOwner(Integer ownerId, boolean isEnabled, String reason, Integer currentAdminId);
}
