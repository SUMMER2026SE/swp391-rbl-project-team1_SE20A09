package com.sportvenue.controller;

import com.sportvenue.dto.request.ComplaintReplyRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.OwnerComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý khiếu nại dành cho Owner.
 * UC-OWN-09: Xem và phản hồi khiếu nại của khách hàng.
 */
@RestController
@RequestMapping("/api/v1/owner/complaints")
@RequiredArgsConstructor
@Tag(name = "Owner — Complaint Management",
     description = "Xem và giải quyết khiếu nại từ khách hàng")
@Slf4j
public class OwnerComplaintController {

    private final OwnerComplaintService ownerComplaintService;

    /**
     * Xem danh sách khiếu nại của tất cả sân.
     * Hỗ trợ filter theo sân cụ thể và phân trang.
     */
    @GetMapping
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Xem danh sách khiếu nại",
               description = "Owner xem tất cả khiếu nại của sân mình. "
                       + "Hỗ trợ filter theo stadiumId.")
    public ResponseEntity<Page<ComplaintResponse>> getComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Integer stadiumId,
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ComplaintResponse> result = ownerComplaintService
                .getOwnerComplaints(
                        userPrincipal.getUser().getUserId(),
                        stadiumId, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * UC-OWN-09: Giải quyết khiếu nại.
     * Owner phản hồi khiếu nại, trạng thái chuyển sang RESOLVED.
     */
    @PutMapping("/{complaintId}/resolve")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Giải quyết khiếu nại",
               description = "Owner phản hồi và giải quyết khiếu nại. "
                       + "Trạng thái chuyển sang RESOLVED.")
    public ResponseEntity<ComplaintResponse> resolveComplaint(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer complaintId,
            @Valid @RequestBody ComplaintReplyRequest request) {

        ComplaintResponse result = ownerComplaintService.resolveComplaint(
                userPrincipal.getUser().getUserId(),
                complaintId, request);
        return ResponseEntity.ok(result);
    }
}
