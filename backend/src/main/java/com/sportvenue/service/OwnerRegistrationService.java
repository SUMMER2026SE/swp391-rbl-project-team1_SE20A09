package com.sportvenue.service;

import com.sportvenue.dto.request.RegisterOwnerRequest;
import com.sportvenue.dto.request.UpgradeToOwnerRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.dto.response.OwnerDetailResponse;
import com.sportvenue.entity.User;

public interface OwnerRegistrationService {
    MessageResponse registerNewOwner(RegisterOwnerRequest request);

    OwnerDetailResponse upgradeCurrentCustomer(User currentUser, UpgradeToOwnerRequest request);

    OwnerDetailResponse getOwnerProfileOfUser(User currentUser);
}
