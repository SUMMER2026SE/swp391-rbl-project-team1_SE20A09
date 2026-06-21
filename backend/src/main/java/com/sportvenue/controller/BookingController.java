package com.sportvenue.controller;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.booking.BookingHistoryItemDto;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.dto.response.WeeklySlotResponse;
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
import com.sportvenue.dto.response.VnpayPaymentUrlResponse;
import com.sportvenue.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    private final PaymentService paymentService;

    @PostMapping("/api/v1/bookings")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Tạo đơn đặt sân đơn lẻ",
            description = "Customer chọn (stadium, slot, reservationDate) và tạo 1 đơn. "
                    + "Server trả về 409 nếu slot đã được đặt active (PENDING_PAYMENT/PENDING/CONFIRMED) "
                    + "trên cùng ngày; 400 nếu slot datetime đã qua hoặc slot không thuộc sân. "
                    + "Booking mới có status=PENDING_PAYMENT, expiredAt = now+5 phút — scheduler sẽ tự huỷ nếu quá hạn.")
    public ResponseEntity<BookingDetailResponse> createBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingDetailResponse response = bookingService.createBooking(userPrincipal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * UC-CUS-01: Xác nhận thanh toán — flip {@code PENDING_PAYMENT} → {@code CONFIRMED},
     * clear {@code expiredAt}, set {@code paymentStatus = PAID}.
     */
    @PostMapping("/api/v1/bookings/{id}/confirm-payment")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Xác nhận thanh toán booking",
            description = "Đổi booking từ PENDING_PAYMENT sang CONFIRMED. Chỉ chủ booking mới được xác nhận. "
                    + "Trả 400 nếu booking đã bị huỷ hoặc đã xác nhận trước đó.")
    public ResponseEntity<BookingDetailResponse> confirmPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Integer bookingId) {
        BookingDetailResponse response = bookingService.confirmPayment(userPrincipal, bookingId);
        return ResponseEntity.ok(response);
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
     * UC-CUS-01: Lịch khung giờ theo tuần của một sân — phục vụ weekly grid UI.
     * Trả về 7 ngày (thứ 2 → chủ nhật) của tuần chứa {@code weekStart}.
     *
     * <p>Public — không yêu cầu auth — đã đăng ký trong
     * {@code SecurityConfig.PUBLIC_ENDPOINTS}.</p>
     *
     * @param stadiumId ID sân
     * @param weekStart một ngày bất kỳ trong tuần (ISO yyyy-MM-dd) — server snap về thứ 2.
     */
    @GetMapping("/api/v1/stadiums/{id}/weekly-slots")
    @Operation(
            summary = "Lịch khung giờ theo tuần của sân — public",
            description = "Trả về 7 ngày × N khung giờ kèm trạng thái "
                    + "AVAILABLE | BOOKED | PAST. BE tự snap về thứ 2 của tuần chứa weekStart. "
                    + "BOOKED = có booking PENDING/CONFIRMED trên (stadiumId, slotId, date). "
                    + "PAST = (date + slot.startTime) < now. KHÔNG trả về customerName.")
    public ResponseEntity<WeeklySlotResponse> getWeeklySlots(
            @PathVariable("id") Integer stadiumId,
            @RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ResponseEntity.ok(bookingService.getWeeklySlots(stadiumId, weekStart));
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

    /**
     * UC-CUS-04: Xem chi tiết một đơn đặt sân theo ID.
     * Chỉ chủ booking mới được xem — 403 nếu không phải.
     */
    @GetMapping("/api/v1/bookings/{id}")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Chi tiết đơn đặt sân",
            description = "Xem chi tiết một đơn đặt sân theo ID. Chỉ chủ booking mới được truy cập.")
    public ResponseEntity<BookingDetailResponse> getBookingDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Integer bookingId) {
        return ResponseEntity.ok(bookingService.getBookingDetail(userPrincipal, bookingId));
    }

    /**
     * UC-CUS-05: Huỷ đơn đặt sân.
     * Chỉ huỷ được khi status là PENDING_PAYMENT / PENDING / CONFIRMED.
     */
    @PutMapping("/api/v1/bookings/{id}/cancel")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Huỷ đơn đặt sân",
            description = "Customer tự huỷ đơn của mình. Không thể huỷ khi đã COMPLETED hoặc CANCELLED.")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Integer bookingId,
            @RequestBody Map<String, String> body) {
        bookingService.cancelBooking(userPrincipal, bookingId, body.get("reason"));
        return ResponseEntity.ok().build();
    }

    /**
     * UC-CUS-02: Sinh URL thanh toán VNPay cho đơn đặt sân PENDING_PAYMENT.
     * Số tiền lấy từ booking.totalPrice trong DB — không nhận từ client.
     */
    @PostMapping("/api/v1/bookings/{id}/pay")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Tạo URL thanh toán VNPay",
            description = "Sinh paymentUrl VNPay cho đơn đặt sân PENDING_PAYMENT. "
                    + "Số tiền lấy từ booking.totalPrice trong DB, không nhận từ request body.")
    public ResponseEntity<VnpayPaymentUrlResponse> payBooking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("id") Integer bookingId,
            @RequestParam(name = "paymentOption", defaultValue = "FULL") String paymentOption,
            jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(paymentService.createVnpayPaymentUrl(userPrincipal, bookingId, paymentOption, request));
    }
}