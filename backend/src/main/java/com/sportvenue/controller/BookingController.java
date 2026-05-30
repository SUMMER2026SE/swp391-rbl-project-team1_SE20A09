package com.sportvenue.controller;

import com.sportvenue.dto.response.BookingResponse;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cho quản lý Booking — dành cho Owner.
 * Base path: /api/v1/owner/bookings
 */
@RestController
@RequestMapping("/api/v1/owner/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Owner Booking Management", description = "API quản lý đặt sân dành cho chủ sân")
@PreAuthorize("hasRole('Owner')")
public class BookingController {

    private final BookingService bookingService;

    /**
     * UC-OWN-06: Xem danh sách booking của tất cả sân thuộc Owner.
     * Hỗ trợ filter theo status và phân trang.
     */
    @GetMapping
    @Operation(
            summary = "Xem tất cả booking của Owner",
            description = "Lấy danh sách booking của tất cả sân thuộc Owner, hỗ trợ filter theo status"
    )
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Filter theo trạng thái booking")
            @RequestParam(required = false) BookingStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng mỗi trang")
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/owner/bookings - userId: {}, status: {}", userPrincipal.getUser().getUserId(), status);
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingResponse> bookings = bookingService.getAllBookingsByOwner(
                userPrincipal.getUser().getUserId(), status, pageable);
        return ResponseEntity.ok(bookings);
    }

    /**
     * UC-OWN-06: Xem danh sách booking của một sân cụ thể.
     * Hỗ trợ filter theo status và phân trang.
     */
    @GetMapping("/by-stadium")
    @Operation(
            summary = "Xem booking theo sân",
            description = "Lấy danh sách booking của một sân cụ thể, Owner phải là chủ sân"
    )
    public ResponseEntity<Page<BookingResponse>> getBookingsByStadium(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "ID sân cần xem booking", required = true)
            @RequestParam Integer stadiumId,
            @Parameter(description = "Filter theo trạng thái booking")
            @RequestParam(required = false) BookingStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng mỗi trang")
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/owner/bookings/by-stadium - userId: {}, stadiumId: {}, status: {}",
                userPrincipal.getUser().getUserId(), stadiumId, status);
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingResponse> bookings = bookingService.getBookingsByStadium(
                userPrincipal.getUser().getUserId(), stadiumId, status, pageable);
        return ResponseEntity.ok(bookings);
    }
}
