package com.sportvenue.service;

import com.sportvenue.dto.request.ApproveOwnerRequest;
import com.sportvenue.dto.request.RegisterOwnerRequest;
import com.sportvenue.dto.request.UpgradeToOwnerRequest;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.response.OwnerDetailResponse;
import com.sportvenue.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OwnerRegistrationService {
    MessageResponse registerNewOwner(RegisterOwnerRequest request);

    OwnerDetailResponse upgradeCurrentCustomer(User currentUser, UpgradeToOwnerRequest request);

    OwnerDetailResponse getOwnerProfileOfUser(User currentUser);

    Page<OwnerDetailResponse> getOwnerRegistrations(ApprovedStatus status, Pageable pageable);

    OwnerDetailResponse approveOrRejectOwner(Integer ownerId, ApproveOwnerRequest request);
}
