package com.sportvenue.controller;

import com.sportvenue.dto.response.CustomerBookingResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Quản lý lịch sử đặt sân của khách hàng")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/bookings/my")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng lấy lịch sử đặt sân của mình")
    public ResponseEntity<List<CustomerBookingResponse>> listCustomerBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<CustomerBookingResponse> response = bookingService.getCustomerBookings(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng hủy đặt sân")
    public ResponseEntity<CustomerBookingResponse> cancelBooking(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CustomerBookingResponse response = bookingService.cancelBooking(id, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/owner/bookings/{id}/confirm")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner xác nhận đơn đặt sân")
    public ResponseEntity<CustomerBookingResponse> confirmBooking(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CustomerBookingResponse response = bookingService.confirmBooking(id, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/owner/bookings/{id}/reject")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner từ chối đơn đặt sân")
    public ResponseEntity<CustomerBookingResponse> rejectBooking(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CustomerBookingResponse response = bookingService.rejectBooking(id, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
