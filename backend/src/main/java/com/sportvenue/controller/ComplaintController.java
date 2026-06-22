package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateComplaintRequest;
import com.sportvenue.dto.request.ReplyComplaintRequest;
import com.sportvenue.dto.request.ResolveComplaintRequest;
import com.sportvenue.dto.response.ComplaintResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Complaint", description = "Quản lý và giải quyết khiếu nại của khách hàng")
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;

    @GetMapping("/owner/complaints")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Lấy danh sách khiếu nại của chủ sân (Owner)")
    public ResponseEntity<Page<ComplaintResponse>> listOwnerComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get owner complaints for: {}", userPrincipal.getUsername());
        return ResponseEntity.ok(complaintService.getOwnerComplaints(userPrincipal.getUsername(), pageable));
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
    public ResponseEntity<Page<ComplaintResponse>> listCustomerComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get customer complaints for: {}", userPrincipal.getUsername());
        return ResponseEntity.ok(complaintService.getCustomerComplaints(userPrincipal.getUsername(), pageable));
    }

    @PostMapping("/complaints")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng tạo khiếu nại mới")
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to create complaint for booking {} by customer: {}", request.getBookingId(), userPrincipal.getUsername());
        ComplaintResponse response = complaintService.createComplaint(request, userPrincipal.getUsername());
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
    public ResponseEntity<Page<ComplaintResponse>> listAdminComplaints(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST request to get all complaints for Admin");
        return ResponseEntity.ok(complaintService.getAllComplaints(pageable));
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
}
