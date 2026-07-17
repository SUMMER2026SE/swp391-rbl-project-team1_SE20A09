package com.sportvenue.controller;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.request.BookingActionRequest;
import com.sportvenue.dto.response.BookingResponse;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.security.RequireApprovedOwner;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.OwnerBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import com.sportvenue.dto.response.WeeklySlotResponse;

/**
 * Controller quản lý đặt sân dành cho Owner.
 * UC-OWN-06: Xem danh sách booking.
 * UC-OWN-07: Xác nhận/Từ chối booking.
 */
@RestController
@RequestMapping("/api/v1/owner/bookings")
@RequiredArgsConstructor
@RequireApprovedOwner
@Tag(name = "Owner — Booking Management",
     description = "Quản lý đặt sân: xem danh sách, xác nhận, từ chối")
@Slf4j
public class OwnerBookingController {

    private final OwnerBookingService ownerBookingService;
    private final BookingService bookingService;

    @GetMapping("/stadiums/{stadiumId}/weekly-slots")
    @PreAuthorize("hasRole('Owner')")
    public ResponseEntity<WeeklySlotResponse> getOwnerWeeklySlots(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer stadiumId,
            @RequestParam LocalDate weekStart) {
        return ResponseEntity.ok(ownerBookingService.getOwnerWeeklySlots(
                principal.getUserId(), stadiumId, weekStart));
    }

    /**
     * UC-OWN-06: Xem danh sách tất cả lịch đặt sân.
     * Hỗ trợ filter theo sân, trạng thái và phân trang.
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Xem danh sách booking",
               description = "Owner xem tất cả booking của sân mình. "
                       + "Hỗ trợ filter theo stadiumId, status và phân trang.")
    public ResponseEntity<Page<BookingResponse>> getBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Integer stadiumId,
            @RequestParam(required = false) BookingStatus status,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 10, sort = "bookingDate",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BookingResponse> result = ownerBookingService.getOwnerBookings(
                userPrincipal.getUser().getUserId(),
                stadiumId, status, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * UC-OWN-07: Xác nhận hoặc từ chối đơn đặt sân.
     * Action: CONFIRM hoặc REJECT (kèm lý do khi reject).
     */
    @PutMapping("/{bookingId}/action")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Xác nhận/Từ chối booking",
               description = "Owner xác nhận hoặc từ chối đơn đặt sân. "
                       + "Khi từ chối phải kèm lý do.")
    public ResponseEntity<BookingResponse> processBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer bookingId,
            @Valid @RequestBody BookingActionRequest request) {

        BookingResponse result = ownerBookingService.processBooking(
                userPrincipal.getUser().getUserId(),
                bookingId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Owner xác nhận ĐÃ THU tiền mặt tại sân cho 1 booking đang {@code AWAITING_CASH_PAYMENT}.
     * Idempotent — gọi lại trên đơn đã {@code PAID} trả về trạng thái hiện tại, không lỗi.
     */
    @PutMapping("/{bookingId}/confirm-cash-payment")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Xác nhận đã thu tiền mặt",
               description = "Owner xác nhận đã thu tiền mặt tại sân cho đơn đang chờ thu tiền — "
                       + "chuyển paymentStatus AWAITING_CASH_PAYMENT sang PAID.")
    public ResponseEntity<BookingDetailResponse> confirmCashPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer bookingId) {

        return ResponseEntity.ok(bookingService.confirmCashPaymentReceived(userPrincipal, bookingId));
    }

    /**
     * Owner xác nhận ĐÃ THU phần còn lại của đơn đặt cọc khi khách tới sân.
     * Idempotent — gọi lại trên đơn đã {@code PAID} trả về trạng thái hiện tại, không lỗi.
     */
    @PutMapping("/{bookingId}/confirm-remaining-payment")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Xác nhận đã thu nốt phần còn lại đơn đặt cọc",
               description = "Owner xác nhận đã thu đủ phần còn lại tại sân cho đơn đang đặt cọc — "
                       + "chuyển paymentStatus DEPOSITED sang PAID.")
    public ResponseEntity<BookingDetailResponse> confirmRemainingPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer bookingId) {

        return ResponseEntity.ok(bookingService.confirmRemainingPaymentReceived(userPrincipal, bookingId));
    }
}
