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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Complaint", description = "Quản lý và giải quyết khiếu nại của khách hàng")
public class ComplaintController {

    private final ComplaintService complaintService;

    @GetMapping("/owner/complaints")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Lấy danh sách khiếu nại của chủ sân (Owner)")
    public ResponseEntity<List<ComplaintResponse>> listOwnerComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ComplaintResponse> response = complaintService.getOwnerComplaints(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/owner/complaints/{id}/reply")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner nhắn phản hồi trong khiếu nại")
    public ResponseEntity<ComplaintResponse> ownerReplyComplaint(
            @PathVariable Integer id,
            @Valid @RequestBody ReplyComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
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
        ComplaintResponse response = complaintService.resolveComplaint(id, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng lấy danh sách khiếu nại của mình")
    public ResponseEntity<List<ComplaintResponse>> listCustomerComplaints(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ComplaintResponse> response = complaintService.getCustomerComplaints(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaints")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng tạo khiếu nại mới")
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
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
        ComplaintResponse response = complaintService.replyComplaint(id, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
