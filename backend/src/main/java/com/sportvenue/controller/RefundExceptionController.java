package com.sportvenue.controller;

import com.sportvenue.dto.request.AdminDecisionRequest;
import com.sportvenue.dto.request.OwnerReviewRequest;
import com.sportvenue.dto.request.SubmitExceptionRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.RefundExceptionResponse;
import com.sportvenue.security.RequireApprovedOwner;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.RefundExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-CUS-1.6 P0: Luồng xét duyệt ngoại lệ hoàn tiền.
 *
 * <p>Endpoints phân theo role:
 * <ul>
 *   <li>Customer: POST /api/v1/refund-exceptions, GET /api/v1/refund-exceptions/me,
 *       PUT /api/v1/refund-exceptions/{id}/escalate,
 *       GET /api/v1/refund-exceptions/booking/{bookingId}/latest</li>
 *   <li>Owner:    PUT /api/v1/owner/refund-exceptions/{id}/review,
 *       GET /api/v1/owner/refund-exceptions</li>
 *   <li>Admin:    PUT /api/v1/admin/refund-exceptions/{id}/decide,
 *       GET /api/v1/admin/refund-exceptions</li>
 * </ul>
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "RefundException", description = "Luồng xét duyệt ngoại lệ hoàn tiền (P0 Mục 1.6)")
public class RefundExceptionController {

    private final RefundExceptionService refundExceptionService;

    // ─────────────────────────────────────────────────────────
    // CUSTOMER endpoints
    // ─────────────────────────────────────────────────────────

    @PostMapping("/api/v1/refund-exceptions")
    @PreAuthorize("hasAnyRole('Customer', 'Owner')")
    @Operation(summary = "Gửi yêu cầu ngoại lệ hoàn tiền", description = "Khách hàng gửi sau khi hủy đơn nhận 0%. Có thể gửi trong 72h kể từ khi hủy.")
    public ResponseEntity<RefundExceptionResponse> submitRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitExceptionRequest req) {
        RefundExceptionResponse response = refundExceptionService.submitRequest(principal.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/refund-exceptions/me")
    @PreAuthorize("hasAnyRole('Customer', 'Owner')")
    @Operation(summary = "Lịch sử yêu cầu ngoại lệ của tôi")
    public ResponseEntity<PageResponse<RefundExceptionResponse>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(refundExceptionService.getCustomerRequests(principal.getUsername(), pageable));
    }

    @PutMapping("/api/v1/refund-exceptions/{id}/escalate")
    @PreAuthorize("hasAnyRole('Customer', 'Owner')")
    @Operation(summary = "Leo thang lên Admin", description = "Chỉ được gọi khi Owner đã từ chối (REJECTED_OWNER) và trong 72h.")
    public ResponseEntity<RefundExceptionResponse> escalate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id) {
        return ResponseEntity.ok(refundExceptionService.customerEscalate(principal.getUsername(), id));
    }

    @GetMapping("/api/v1/refund-exceptions/booking/{bookingId}/latest")
    @PreAuthorize("hasAnyRole('Customer', 'Owner', 'Admin')")
    @Operation(summary = "Lấy yêu cầu mới nhất của 1 booking", description = "Dùng để hiển thị badge trạng thái trên UI chi tiết đơn.")
    public ResponseEntity<RefundExceptionResponse> getLatestByBooking(@PathVariable Integer bookingId) {
        RefundExceptionResponse res = refundExceptionService.getLatestByBookingId(bookingId);
        return res != null ? ResponseEntity.ok(res) : ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────
    // OWNER endpoints
    // ─────────────────────────────────────────────────────────

    @GetMapping("/api/v1/owner/refund-exceptions")
    @RequireApprovedOwner
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Danh sách yêu cầu ngoại lệ chờ Owner duyệt")
    public ResponseEntity<PageResponse<RefundExceptionResponse>> getOwnerPending(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(refundExceptionService.getOwnerPendingRequests(principal.getUsername(), pageable));
    }

    @PutMapping("/api/v1/owner/refund-exceptions/{id}/review")
    @RequireApprovedOwner
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner duyệt (chấp nhận/từ chối) yêu cầu ngoại lệ",
               description = "approved=true → refundPercent phải là 50 hoặc 100. approved=false → refundPercent phải null.")
    public ResponseEntity<RefundExceptionResponse> ownerReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @Valid @RequestBody OwnerReviewRequest req) {
        return ResponseEntity.ok(refundExceptionService.ownerReview(principal.getUsername(), id, req));
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN endpoints
    // ─────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/refund-exceptions")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Hàng đợi Admin — yêu cầu ngoại lệ cần xem xét")
    public ResponseEntity<PageResponse<RefundExceptionResponse>> getAdminQueue(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(refundExceptionService.getAdminQueue(pageable));
    }

    @PutMapping("/api/v1/admin/refund-exceptions/{id}/decide")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin ra quyết định cuối về yêu cầu ngoại lệ",
               description = "approved=true → refundPercent phải là 50 hoặc 100. approved=false → từ chối cuối, kết thúc luồng.")
    public ResponseEntity<RefundExceptionResponse> adminDecide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer id,
            @Valid @RequestBody AdminDecisionRequest req) {
        return ResponseEntity.ok(refundExceptionService.adminDecide(principal.getUsername(), id, req));
    }
}
