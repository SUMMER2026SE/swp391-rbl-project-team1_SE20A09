package com.sportvenue.service;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AccountStatus;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    PageResponse<AdminCustomerResponse> getCustomers(String search, AccountStatus accountStatus, Pageable pageable);
    void lockUnlockCustomer(Integer id, Boolean enabled, Integer currentAdminId);
}
