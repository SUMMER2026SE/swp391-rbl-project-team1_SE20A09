package com.sportvenue.service;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AccountStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface cho các chức năng quản trị user của Admin.
 * UC-ADM-02: View List Customer
 * UC-ADM-03: Lock/Unlock Customer
 * UC-ADM-04: View List Owner
 * UC-ADM-05: Lock/Unlock Owner
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

    /**
     * UC-ADM-03: Khoá hoặc mở khoá tài khoản khách hàng.
     * @param id ID của khách hàng
     * @param enabled true = ACTIVE, false = BLOCKED
     * @param currentAdminId ID của admin đang thao tác
     */
    void lockUnlockCustomer(Integer id, Boolean enabled, Integer currentAdminId);

    /**
     * Lấy danh sách chủ sân (role=Owner) có phân trang, tìm kiếm và lọc trạng thái.
     *
     * @param search       từ khóa tìm kiếm theo tên, email, SĐT (nullable)
     * @param accountStatus lọc theo trạng thái tài khoản: ACTIVE, BLOCKED, PENDING (nullable)
     * @param pageable     thông tin phân trang (page, size, sort)
     * @return PageResponse chứa danh sách AdminCustomerResponse
     */
    PageResponse<AdminCustomerResponse> getOwners(String search, AccountStatus accountStatus, Pageable pageable);

    /**
     * UC-ADM-05: Khoá hoặc mở khoá tài khoản chủ sân.
     * @param id ID của chủ sân
     * @param enabled true = ACTIVE, false = BLOCKED
     * @param currentAdminId ID của admin đang thao tác
     */
    void lockUnlockOwner(Integer id, Boolean enabled, Integer currentAdminId);
}
