package com.sportvenue.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.request.ReplyComplaintRequest;
import com.sportvenue.dto.request.ResolveComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ComplaintEscalationService;
import com.sportvenue.service.ComplaintService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Complaint", description = "Quản lý và giải quyết khiếu nại của khách hàng")
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintEscalationService escalationService;

    @GetMapping("/owner/complaints")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Lấy danh sách khiếu nại của chủ sân (Owner)")
    public ResponseEntity<PageResponse<ComplaintResponse>> listOwnerComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get owner complaints for: {}", userPrincipal.getUsername());
        return ResponseEntity.ok(PageResponse.of(complaintService.getOwnerComplaints(userPrincipal.getUsername(), pageable)));
    }

    @PostMapping("/owner/complaints/{id}/reply")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner nhắn phản hồi trong khiếu nại")
    public ResponseEntity<ComplaintResponse> ownerReplyComplaint(
            @PathVariable Integer id,
            @Valid @RequestBody ReplyComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to reply to complaint {} by owner: {}", id, userPrincipal.getUsername());
        ComplaintResponse response = complaintService.replyComplaint(id, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/owner/complaints/{id}/resolve")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner đóng giải quyết khiếu nại")
    public ResponseEntity<ComplaintResponse> resolveComplaint(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to resolve complaint {} by owner: {}", id, userPrincipal.getUsername());
        ComplaintResponse response = complaintService.resolveComplaint(id, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng lấy danh sách khiếu nại của mình")
    public ResponseEntity<PageResponse<ComplaintResponse>> listCustomerComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get customer complaints for: {}", userPrincipal.getUsername());
        return ResponseEntity.ok(PageResponse.of(complaintService.getCustomerComplaints(userPrincipal.getUsername(), pageable)));
    }

    @PostMapping("/complaints")
    @PreAuthorize("hasAnyRole('Customer', 'Owner', 'Admin')")
    @Operation(summary = "Người dùng tạo khiếu nại mới (Hệ thống hoặc Đặt sân)")
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String username = userPrincipal != null ? userPrincipal.getUsername() : "anonymousUser";
        log.info("REST request to create complaint for booking {} by customer: {}", request.getBookingId(), username);
        ComplaintResponse response = complaintService.createComplaint(request, username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaints/{id}/reply")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng nhắn phản hồi trong khiếu nại")
    public ResponseEntity<ComplaintResponse> customerReplyComplaint(
            @PathVariable Integer id,
            @Valid @RequestBody ReplyComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to reply to complaint {} by customer: {}", id, userPrincipal.getUsername());
        ComplaintResponse response = complaintService.replyComplaint(id, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaints/{id}/close")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng tự đóng khiếu nại của mình")
    public ResponseEntity<ComplaintResponse> closeComplaint(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to close complaint {} by customer: {}", id, userPrincipal.getUsername());
        ComplaintResponse response = complaintService.closeComplaint(id, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/complaints")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin lấy danh sách toàn bộ khiếu nại trong hệ thống")
    public ResponseEntity<PageResponse<ComplaintResponse>> listAdminComplaints(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get all complaints for Admin");
        return ResponseEntity.ok(PageResponse.of(complaintService.getAllComplaints(pageable)));
    }

    @PostMapping("/admin/complaints/{id}/reply")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin nhắn phản hồi trong khiếu nại")
    public ResponseEntity<ComplaintResponse> adminReplyComplaint(
            @PathVariable Integer id,
            @Valid @RequestBody ReplyComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to reply to complaint {} by admin: {}", id, userPrincipal.getUsername());
        ComplaintResponse response = complaintService.replyComplaint(id, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/complaints/{id}/resolve")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin đóng và giải quyết khiếu nại")
    public ResponseEntity<ComplaintResponse> adminResolveComplaint(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveComplaintRequest request) {
        log.info("REST request to resolve complaint {} by admin", id);
        ComplaintResponse response = complaintService.resolveComplaintByAdmin(id, request);
        return ResponseEntity.ok(response);
    }

    // NEW ESCALATION ENDPOINTS
    
    @PostMapping("/complaints/{id}/escalate")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng escalate khiếu nại lên Admin")
    public ResponseEntity<Void> customerEscalateComplaint(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String reason = body.getOrDefault("reason", "Không hài lòng với cách xử lý của chủ sân");
        escalationService.escalateToAdmin(id, reason, userPrincipal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complaints/{id}/object")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng phản đối giải pháp của Owner")
    public ResponseEntity<Void> customerObjectToResolution(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String objectionReason = body.getOrDefault("reason", "Không đồng ý với giải pháp");
        escalationService.customerObjectToResolution(id, objectionReason, userPrincipal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/complaints/{id}/approve")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin chấp nhận giải pháp của Owner")
    public ResponseEntity<Void> adminApproveResolution(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        escalationService.adminApproveResolution(id, userPrincipal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/complaints/{id}/override")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin ghi đè giải pháp của Owner")
    public ResponseEntity<Void> adminOverrideResolution(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String newResolution = body.get("resolution");
        escalationService.adminOverrideResolution(id, newResolution, userPrincipal.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/complaints/escalated")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin xem danh sách khiếu nại đã escalate")
    public ResponseEntity<PageResponse<ComplaintResponse>> listEscalatedComplaints(
            @PageableDefault(size = 20, sort = "escalatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get escalated complaints for Admin");
        // Sẽ implement method này trong ComplaintService
        return ResponseEntity.ok(PageResponse.of(complaintService.getEscalatedComplaints(pageable)));
    }

    @GetMapping("/admin/complaints/stats")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin lấy thống kê khiếu nại")
    public ResponseEntity<com.sportvenue.dto.response.ComplaintStatsDto> getAdminComplaintStats() {
        return ResponseEntity.ok(complaintService.getAdminComplaintStats());
    }
}
