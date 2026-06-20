package com.sportvenue.controller;

import com.sportvenue.dto.booking.CreateCustomerRecurringBookingRequest;
import com.sportvenue.dto.booking.CustomerBookingDetailDto;
import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.booking.CustomerRecurringBookingResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.CustomerBookingService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Lịch sử đặt sân của khách hàng")
public class CustomerBookingController {

    private final CustomerBookingService customerBookingService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('Customer')")
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Chi tiết đơn đặt sân",
            description = "Xem chi tiết một đơn đặt sân theo ID")
    public ResponseEntity<CustomerBookingDetailDto> getBookingDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id) {
        return ResponseEntity.ok(customerBookingService.getBookingDetail(userPrincipal, id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Huỷ đơn đặt sân",
            description = "Khách hàng tự huỷ đơn đặt sân của mình nếu chưa hoàn thành")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        customerBookingService.cancelBooking(userPrincipal, id, request.get("reason"));
        return ResponseEntity.ok().build();
    }

    /**
     * UC-CUS-01: Tạo chuỗi đặt sân định kỳ.
     * Trả về danh sách đơn đã tạo hoặc 400 nếu có slot bị trùng (all-or-nothing).
     */
    @PostMapping("/recurring")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Đặt sân định kỳ",
            description = "Tạo N đơn đặt sân cùng chuỗi theo khoảng ngày + thứ trong tuần + khung giờ. "
                    + "Cơ chế all-or-nothing: nếu có bất kỳ (date, slot) nào bị trùng, trả về 400 và không tạo đơn nào.")
    public ResponseEntity<CustomerRecurringBookingResponse> createRecurringBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateCustomerRecurringBookingRequest request) {
        return ResponseEntity.ok(
                customerBookingService.createRecurringBooking(userPrincipal, request));
    }
}