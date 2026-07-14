package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Controller quản trị users — chỉ dành cho Admin.
 * UC-ADM-02: GET /api/v1/admin/customers — Danh sách khách hàng.
 * UC-ADM-03, UC-ADM-04, UC-ADM-05 sẽ bổ sung sau.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
@Tag(name = "Admin User Management", description = "Quản lý danh sách người dùng — chỉ dành cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * UC-ADM-02: Lấy danh sách khách hàng (role=Customer) có phân trang.
     *
     * @param page          số trang (0-indexed), mặc định 0
     * @param pageSize      kích thước trang, mặc định 10
     * @param search        từ khóa tìm kiếm theo tên, email, SĐT (tùy chọn)
     * @param accountStatus lọc theo trạng thái: ACTIVE, BLOCKED, PENDING (tùy chọn)
     * @param sortBy        trường sắp xếp, mặc định "createdAt"
     * @param sortDir       hướng sắp xếp: asc hoặc desc, mặc định "desc"
     * @return 200 OK với danh sách khách hàng phân trang
     */
    @GetMapping("/customers")
    @Operation(
        summary = "Xem danh sách khách hàng",
        description = "Admin xem danh sách tất cả khách hàng (role=Customer). "
                    + "Hỗ trợ phân trang, tìm kiếm theo tên/email/SĐT và lọc theo trạng thái tài khoản."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Lấy danh sách khách hàng thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Không có quyền — yêu cầu role Admin")
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminCustomerResponse>>> getCustomers(
            @Parameter(description = "Số trang (0-indexed)", example = "0")
            @jakarta.validation.constraints.Min(value = 0, message = "Số trang (page) không được nhỏ hơn 0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang", example = "10")
            @jakarta.validation.constraints.Min(value = 1, message = "Số bản ghi mỗi trang (pageSize) phải lớn hơn 0")
            @RequestParam(defaultValue = "10") int pageSize,

            @Parameter(description = "Tìm kiếm theo tên, email hoặc SĐT")
            @RequestParam(required = false) String search,

            @Parameter(description = "Lọc theo trạng thái tài khoản: ACTIVE, BLOCKED, PENDING")
            @RequestParam(required = false) AccountStatus accountStatus,

            @Parameter(description = "Trường sắp xếp", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Hướng sắp xếp: asc hoặc desc", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        List<String> allowedSortBy = List.of(
                "userId", "firstName", "lastName", "email",
                "phoneNumber", "accountStatus", "createdAt"
        );
        if (!allowedSortBy.contains(sortBy)) {
            sortBy = "createdAt"; // fallback an toàn
        }

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        PageResponse<AdminCustomerResponse> result = adminUserService.getCustomers(search, accountStatus, pageable);

        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminCustomerResponse>>builder()
                .code(200)
                .message("Lấy danh sách khách hàng thành công")
                .result(result)
                .build());
    }

    /**
     * UC-ADM-03: Khóa/Mở khóa tài khoản khách hàng.
     */
    @org.springframework.web.bind.annotation.PatchMapping("/customers/{id}/lock")
    @Operation(summary = "Khóa/Mở khóa khách hàng", description = "Thay đổi trạng thái ACTIVE/BLOCKED của khách hàng")
    public ResponseEntity<ApiResponse<Void>> lockUnlockCustomer(
            @Parameter(description = "ID của khách hàng") @org.springframework.web.bind.annotation.PathVariable Integer id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.sportvenue.dto.request.LockCustomerRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.sportvenue.security.UserPrincipal currentUser) {

        adminUserService.lockUnlockCustomer(id, request.getEnabled(), currentUser.getUser().getUserId(), request.getReason());

        String statusMessage = Boolean.TRUE.equals(request.getEnabled()) ? "mở khóa" : "khóa";
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Đã " + statusMessage + " tài khoản thành công")
                .build());
    }
}
