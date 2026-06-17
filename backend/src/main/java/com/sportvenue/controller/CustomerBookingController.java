package com.sportvenue.controller;

import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.CustomerBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Lịch sử đặt sân của khách hàng")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;

    @GetMapping("/me")
    @Operation(
            summary = "Lịch sử đặt sân của tôi",
            description = "Danh sách đơn đặt sân từ database, sắp xếp mới nhất trước, hỗ trợ phân trang và lọc theo trạng thái")
    public ResponseEntity<PageResponse<CustomerBookingHistoryDto>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(customerBookingService.getMyBookings(userPrincipal, status, page, size));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/cancel")
    @Operation(
            summary = "Huỷ đơn đặt sân",
            description = "Khách hàng tự huỷ đơn đặt sân của mình nếu chưa hoàn thành")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> request) {
        customerBookingService.cancelBooking(userPrincipal, id, request.get("reason"));
        return ResponseEntity.ok().build();
    }
}
