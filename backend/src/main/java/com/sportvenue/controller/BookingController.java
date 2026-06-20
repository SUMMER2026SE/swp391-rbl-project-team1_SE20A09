package com.sportvenue.controller;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.booking.BookingHistoryItemDto;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-CUS-01: Booking controller cho Customer — single booking flow.
 *
 * <ul>
 *   <li>{@code POST /api/v1/bookings} — tạo đơn đặt sân đơn lẻ (single booking).</li>
 *   <li>{@code GET  /api/v1/stadiums/{id}/slots?date=YYYY-MM-DD} — liệt kê slot kèm
 *       cờ availability để FE render UI.</li>
 *   <li>{@code GET  /api/v1/bookings/me?page=&size=&status=} — lịch sử đặt sân
 *       của customer hiện tại (lấy user từ SecurityContext / JWT).</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Booking (Customer)", description = "Single booking flow cho khách hàng (UC-CUS-01)")
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/api/v1/bookings")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Tạo đơn đặt sân đơn lẻ",
            description = "Customer chọn (stadium, slot, reservationDate) và tạo 1 đơn. "
                    + "Server trả về 409 nếu slot đã được đặt active (PENDING/CONFIRMED) "
                    + "trên cùng ngày; 400 nếu slot datetime đã qua hoặc slot không thuộc sân.")
    public ResponseEntity<BookingDetailResponse> createBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingDetailResponse response = bookingService.createBooking(userPrincipal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/stadiums/{id}/slots")
    @Operation(
            summary = "Liệt kê slot của sân theo ngày — kèm cờ availability",
            description = "Mỗi slot có cờ `available`: true nếu slot còn trống và chưa qua; "
                    + "false nếu đã có booking PENDING/CONFIRMED trên ngày đó hoặc đã qua giờ.")
    public ResponseEntity<List<TimeSlotResponse>> getStadiumSlotsByDate(
            @PathVariable("id") Integer stadiumId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getSlotsByDate(stadiumId, date));
    }

    /**
     * UC-CUS-01: Lịch sử đặt sân của customer hiện tại — phục vụ tab "Lịch sử đặt sân".
     * User lấy từ {@link UserPrincipal} trong SecurityContext (qua JWT).
     *
     * @param userPrincipal customer đang đăng nhập (inject tự động bởi Spring Security).
     * @param page          trang (0-based, mặc định 0).
     * @param size          kích thước trang (mặc định 10).
     * @param status        bộ lọc trạng thái — FE truyền {@code all|upcoming|completed|cancelled}
     *                      hoặc để trống để lấy tất cả.
     */
    @GetMapping("/api/v1/bookings/me")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Lịch sử đặt sân của customer hiện tại",
            description = "Trả về danh sách booking (có phân trang) của user đang đăng nhập. "
                    + "User lấy từ JWT — không cần truyền userId. "
                    + "Filter `status`: all | upcoming (PENDING/CONFIRMED) | completed | cancelled.")
    public ResponseEntity<PageResponse<BookingHistoryItemDto>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(
                bookingService.getMyBookings(userPrincipal, page, size, status));
    }
}