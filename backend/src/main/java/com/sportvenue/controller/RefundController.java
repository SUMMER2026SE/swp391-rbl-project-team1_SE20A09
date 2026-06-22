package com.sportvenue.controller;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.OwnerBookingResponse;
import com.sportvenue.dto.response.RefundResponse;
import com.sportvenue.security.RequireApprovedOwner;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.RefundService;
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
@RequestMapping("/api/v1/owner/bookings")
@RequiredArgsConstructor
@RequireApprovedOwner
@Tag(name = "Refund", description = "Quản lý và xử lý hoàn tiền đặt sân (Owner)")
public class RefundController {

    private final RefundService refundService;

    @GetMapping
    @PreAuthorize("hasRole('Owner')")
    @Operation(
            summary = "Lấy danh sách tất cả đơn đặt sân của chủ sân (Owner)",
            description = "Yêu cầu tài khoản đăng nhập có vai trò Owner. Trả về toàn bộ danh sách đơn đặt sân để hiển thị trên Dashboard."
    )
    public ResponseEntity<List<OwnerBookingResponse>> listOwnerBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<OwnerBookingResponse> response = refundService.getOwnerBookings(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingId}/refund/preview")
    @PreAuthorize("hasRole('Owner')")
    @Operation(
            summary = "Xem trước số tiền hoàn lại trước khi xác nhận hủy sân",
            description = "Yêu cầu tài khoản đăng nhập có vai trò Owner và sở hữu đúng sân này. Trả về số tiền hoàn dự kiến dựa trên thời gian hiện tại của server."
    )
    public ResponseEntity<RefundResponse> previewRefund(
            @PathVariable Integer bookingId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        RefundResponse response = refundService.previewRefund(bookingId, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

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
