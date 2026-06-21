package com.sportvenue.controller;

import com.sportvenue.dto.request.ApproveOwnerRequest;
import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.OwnerDetailResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.service.AdminOwnerService;
import com.sportvenue.service.OwnerRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/owners")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Owner Management", description = "Quản lý và phê duyệt hồ sơ đối tác chủ sân — chỉ dành cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminOwnerController {

    private final OwnerRegistrationService ownerRegistrationService;
    private final AdminOwnerService adminOwnerService;

    /**
     * Lấy danh sách tất cả chủ sân (Owner) phục vụ quản lý tài khoản (Active/Locked).
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách chủ sân",
            description = "Admin lấy danh sách chủ sân có lọc theo từ khóa tìm kiếm, trạng thái tài khoản, trạng thái duyệt."
    )
    public ResponseEntity<ApiResponse<PageResponse<AdminOwnerResponse>>> getOwners(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(required = false) String approvedStatus,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Validate sorting column to prevent SQL injection
        List<String> allowedSortBy = List.of("fullName", "email", "phoneNumber", "businessName", "createdAt");
        if (!allowedSortBy.contains(sortBy)) {
            sortBy = "createdAt";
        }
        
        PageResponse<AdminOwnerResponse> result = adminOwnerService.getOwners(search, accountStatus, approvedStatus, page, size, sortBy, sortDir);
        
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminOwnerResponse>>builder()
                .code(200)
                .message("Lấy danh sách Owner thành công")
                .result(result)
                .build());
    }

    /**
     * Lấy danh sách hồ sơ đối tác phân trang theo trạng thái phê duyệt (dành riêng cho luồng Duyệt hồ sơ).
     */
    @GetMapping("/registrations")
    @Operation(
            summary = "Xem danh sách hồ sơ đăng ký chủ sân",
            description = "Admin xem danh sách các hồ sơ chủ sân phân trang theo trạng thái phê duyệt: PENDING, APPROVED, REJECTED."
    )
    public ResponseEntity<ApiResponse<PageResponse<OwnerDetailResponse>>> getOwnerRegistrations(
            @Parameter(description = "Trạng thái phê duyệt: PENDING, APPROVED, REJECTED", example = "PENDING")
            @RequestParam(defaultValue = "PENDING") ApprovedStatus status,

            @Parameter(description = "Số trang (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,

            @Parameter(description = "Trường sắp xếp", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Hướng sắp xếp: asc hoặc desc", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        List<String> allowedSortBy = List.of("ownerId", "businessName", "taxCode", "createdAt");
        if (!allowedSortBy.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Page<OwnerDetailResponse> pageResult = ownerRegistrationService.getOwnerRegistrations(status, pageable);
        PageResponse<OwnerDetailResponse> result = PageResponse.of(pageResult);

        return ResponseEntity.ok(ApiResponse.<PageResponse<OwnerDetailResponse>>builder()
                .code(200)
                .message("Lấy danh sách hồ sơ đối tác thành công.")
                .result(result)
                .build());
    }

    /**
     * Phê duyệt hoặc từ chối hồ sơ đối tác.
     */
    @PostMapping("/{ownerId}/approve")
    @Operation(
            summary = "Phê duyệt hoặc từ chối hồ sơ đối tác",
            description = "Cập nhật trạng thái duyệt hồ sơ thành APPROVED hoặc REJECTED. Nếu từ chối phải ghi rõ lý do."
    )
    public ResponseEntity<ApiResponse<OwnerDetailResponse>> approveOrRejectOwner(
            @Parameter(description = "ID của hồ sơ đối tác", example = "1")
            @PathVariable Integer ownerId,

            @Valid @RequestBody ApproveOwnerRequest request
    ) {
        OwnerDetailResponse response = ownerRegistrationService.approveOrRejectOwner(ownerId, request);
        String action = request.getApprovedStatus() == ApprovedStatus.APPROVED ? "Phê duyệt" : "Từ chối";

        return ResponseEntity.ok(ApiResponse.<OwnerDetailResponse>builder()
                .code(200)
                .message(action + " hồ sơ đối tác thành công.")
                .result(response)
                .build());
    }
}
