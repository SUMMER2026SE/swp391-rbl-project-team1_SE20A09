package com.sportvenue.controller;

import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.CustomerBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
            description = "Danh sách đơn đặt sân từ database, sắp xếp mới nhất trước")
    public ResponseEntity<List<CustomerBookingHistoryDto>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(customerBookingService.getMyBookings(userPrincipal));
    }
}
