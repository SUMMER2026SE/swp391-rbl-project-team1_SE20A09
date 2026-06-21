package com.sportvenue.service;

import com.sportvenue.dto.booking.BookingDetailResponse;
import com.sportvenue.dto.booking.BookingHistoryItemDto;
import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.dto.response.WeeklySlotResponse;
import com.sportvenue.security.UserPrincipal;

import java.time.LocalDate;
import java.util.List;

/**
 * UC-CUS-01: Single booking service cho Customer.
 *
 * <ul>
 *   <li>{@link #createBooking} — tạo một đơn đặt sân đơn lẻ.</li>
 *   <li>{@link #getSlotsByDate} — liệt kê khung giờ của sân kèm cờ
 *       {@code available} cho một ngày cụ thể (FE dùng để render UI).</li>
 *   <li>{@link #getMyBookings} — lịch sử đặt sân của customer hiện tại
 *       (có phân trang + lọc trạng thái), phục vụ {@code GET /api/v1/bookings/me}.</li>
 * </ul>
 */
public interface BookingService {

    /**
     * Tạo booking đơn lẻ cho customer hiện tại.
     *
     * @throws com.sportvenue.exception.BadRequestException
     *         nếu sân / slot không tồn tại, slot không thuộc sân, hoặc
     *         slot datetime đã qua.
     * @throws com.sportvenue.exception.DuplicateResourceException
     *         (→ 409) nếu đã có booking PENDING/CONFIRMED cho cùng
     *         (stadiumId, slotId, reservationDate).
     */
    BookingDetailResponse createBooking(UserPrincipal principal, CreateBookingRequest request);

    /**
     * Liệt kê slot của sân kèm availability cho {@code date}.
     * Một slot được coi là unavailable nếu:
     * <ul>
     *   <li>Đã có booking PENDING/CONFIRMED cho ngày đó; hoặc</li>
     *   <li>Datetime bắt đầu (date + slot.startTime) đã qua so với hiện tại.</li>
     * </ul>
     */
    List<TimeSlotResponse> getSlotsByDate(Integer stadiumId, LocalDate date);

    /**
     * Lấy lịch sử đặt sân của customer hiện tại — có phân trang, lọc theo trạng thái.
     * Phục vụ {@code GET /api/v1/bookings/me}.
     *
     * <p>Mapping {@code statusFilter} (FE → BE):</p>
     * <ul>
     *   <li>{@code null} / rỗng / {@code "all"} → trả tất cả trạng thái.</li>
     *   <li>{@code "upcoming"}  → PENDING, CONFIRMED.</li>
     *   <li>{@code "completed"} → COMPLETED.</li>
     *   <li>{@code "cancelled"} → CANCELLED.</li>
     *   <li>{@code "pending"}   → PENDING.</li>
     *   <li>{@code "confirmed"} → CONFIRMED.</li>
     *   <li>Giá trị khác → fallback về tất cả trạng thái (không throw).</li>
     * </ul>
     *
     * @param principal    customer đang đăng nhập (lấy từ SecurityContext / JWT).
     * @param page         trang (0-based), số âm sẽ được clamp về 0.
     * @param size         kích thước trang (tối thiểu 1).
     * @param statusFilter bộ lọc trạng thái (xem mapping ở trên).
     */
    PageResponse<BookingHistoryItemDto> getMyBookings(
            UserPrincipal principal,
            int page,
            int size,
            String statusFilter);

    /**
     * UC-CUS-01: Lịch khung giờ theo tuần của một sân — phục vụ
     * {@code GET /api/v1/stadiums/{id}/weekly-slots?weekStart=YYYY-MM-DD}.
     *
     * <p>Trả về 7 ngày (thứ 2 → chủ nhật) của tuần chứa {@code weekStart}.
     * {@code weekStart} được snap về thứ 2 gần nhất nếu FE truyền ngày khác.</p>
     *
     * <p>Trạng thái của mỗi slot (theo từng ngày):</p>
     * <ul>
     *   <li>{@code BOOKED}   — đã có booking PENDING/CONFIRMED cho (stadiumId, slotId, date).</li>
     *   <li>{@code PAST}     — datetime bắt đầu (date + slot.startTime) đã qua so với hiện tại.</li>
     *   <li>{@code AVAILABLE} — còn lại.</li>
     * </ul>
     *
     * <p>KHÔNG trả về {@code customerName} hay thông tin khách hàng.</p>
     */
    WeeklySlotResponse getWeeklySlots(Integer stadiumId, LocalDate weekStart);

    /**
     * UC-CUS-01: Xác nhận thanh toán cho một booking đang ở {@code PENDING_PAYMENT}.
     *
     * <p>Chỉ chủ booking (user.customer) mới được xác nhận. Đổi status sang
     * {@code CONFIRMED}, set {@code paymentStatus = PAID}, clear {@code expiredAt}.</p>
     *
     * @throws com.sportvenue.exception.ResourceNotFoundException
     *         nếu booking không tồn tại.
     * @throws com.sportvenue.exception.BadRequestException
     *         nếu booking đã bị huỷ hoặc đã được xác nhận trước đó.
     */
    BookingDetailResponse confirmPayment(UserPrincipal principal, Integer bookingId);

    /**
     * UC-CUS-04: Xem chi tiết một đơn đặt sân theo ID.
     * Chỉ chủ booking mới được xem — trả 403 nếu userId không khớp.
     *
     * @throws com.sportvenue.exception.ResourceNotFoundException nếu booking không tồn tại.
     */
    BookingDetailResponse getBookingDetail(UserPrincipal principal, Integer bookingId);

    /**
     * UC-CUS-05: Huỷ đơn đặt sân của customer.
     * Chỉ huỷ được khi status là PENDING_PAYMENT, PENDING hoặc CONFIRMED.
     * Không thể huỷ khi đã COMPLETED hoặc đã CANCELLED.
     *
     * @throws com.sportvenue.exception.ResourceNotFoundException nếu booking không tồn tại.
     * @throws com.sportvenue.exception.BadRequestException       nếu không thể huỷ.
     */
    void cancelBooking(UserPrincipal principal, Integer bookingId, String reason);
}