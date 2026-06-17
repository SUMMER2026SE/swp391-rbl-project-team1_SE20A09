package com.sportvenue.service;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AccountStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface cho các chức năng quản trị user của Admin.
 * UC-ADM-02: View List Customer
 * UC-ADM-03: Lock/Unlock Customer (sẽ bổ sung sau)
 * UC-ADM-04: View List Owner (sẽ bổ sung sau)
 * UC-ADM-05: Lock/Unlock Owner (sẽ bổ sung sau)
 */
public interface AdminUserService {

    /**
     * Lấy danh sách khách hàng (role=Customer) có phân trang, tìm kiếm và lọc trạng thái.
     *
     * @param search       từ khóa tìm kiếm theo tên, email, SĐT (nullable)
     * @param accountStatus lọc theo trạng thái tài khoản: ACTIVE, BLOCKED, PENDING (nullable)
     * @param pageable     thông tin phân trang (page, size, sort)
     * @return PageResponse chứa danh sách AdminCustomerResponse
     */
    PageResponse<AdminCustomerResponse> getCustomers(String search, AccountStatus accountStatus, Pageable pageable);
}
