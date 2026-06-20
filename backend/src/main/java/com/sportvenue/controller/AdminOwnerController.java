package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.service.AdminOwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/owners")
@RequiredArgsConstructor
public class AdminOwnerController {

    private final AdminOwnerService adminOwnerService;

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
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
}
