package com.sportvenue.controller;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.RefundResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/bookings")
@RequiredArgsConstructor
@Tag(name = "Refund", description = "Quản lý và xử lý hoàn tiền đặt sân (Owner)")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/{bookingId}/refund")
    @PreAuthorize("hasRole('Owner')")
    @Operation(
            summary = "Xử lý hoàn tiền khi hủy đặt sân",
            description = "Yêu cầu tài khoản đăng nhập có vai trò Owner và sở hữu đúng sân này. Cần gửi Bearer JWT Token ở Header"
    )
    public ResponseEntity<RefundResponse> processRefund(
            @PathVariable Integer bookingId,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        RefundResponse response = refundService.processRefund(bookingId, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
