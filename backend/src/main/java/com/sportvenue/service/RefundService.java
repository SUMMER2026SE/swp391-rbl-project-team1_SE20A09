package com.sportvenue.service;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.OwnerBookingResponse;
import com.sportvenue.dto.response.OwnerBookingsSummaryResponse;
import com.sportvenue.dto.response.RefundResponse;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.RefundReasonType;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundService {
    
    /**
     * Xử lý hoàn tiền khi hủy đặt sân (Owner).
     *
     * @param bookingId ID của đơn đặt sân cần hoàn tiền
     * @param request Yêu cầu chứa lý do hoàn tiền
     * @param ownerEmail Email của Owner đang thực hiện thao tác
     * @return Kết quả hoàn tiền chi tiết
     */
    RefundResponse processRefund(Integer bookingId, RefundRequest request, String ownerEmail);

    /**
     * Xem trước số tiền hoàn lại trước khi xác nhận hủy sân (Owner).
     *
     * @param bookingId ID của đơn đặt sân cần xem trước hoàn tiền
     * @param reasonType Nguyên nhân hoàn tiền (Khách yêu cầu hay do sự cố sân)
     * @param ownerEmail Email của Owner đang thực hiện thao tác
     * @return Kết quả hoàn tiền ước tính chi tiết
     */
    RefundResponse previewRefund(Integer bookingId, RefundReasonType reasonType, String ownerEmail);

    /**
     * Xem trước số tiền hoàn lại trước khi xác nhận hủy sân (Customer).
     *
     * @param bookingId ID của đơn đặt sân
     * @param customerEmail Email của Customer
     * @return Kết quả hoàn tiền ước tính chi tiết
     */
    RefundResponse previewRefundForCustomer(Integer bookingId, String customerEmail);

    /**
     * Lấy trạng thái/kết quả hoàn tiền hiện tại của một đơn (dùng cho idempotency replay).
     *
     * @param bookingId ID của đơn đặt sân
     * @param ownerEmail Email của Owner
     * @return Kết quả hoàn tiền chi tiết
     */
    RefundResponse getRefundResponse(Integer bookingId, String ownerEmail);

    /**
     * Lấy danh sách đặt sân có phân trang của Owner.
     *
     * @param ownerEmail Email của Owner đang thực hiện thao tác
     * @param status trạng thái cần lọc, null để lấy tất cả
     * @param pageable thông tin phân trang
     * @return Trang DTO chứa thông tin đặt sân
     */
    Page<OwnerBookingResponse> getOwnerBookings(
            String ownerEmail, Integer stadiumId, LocalDate startDate, LocalDate endDate, BookingStatus status, Pageable pageable);

    /**
     * Tổng hợp Gross/Refund/Fee/Net trên TOÀN BỘ booking của Owner (không phụ thuộc phân trang) —
     * dùng cho card summary, tránh bug tổng bị lệch khi chỉ cộng dồn 1 trang đang hiển thị.
     *
     * @param ownerEmail Email của Owner đang thực hiện thao tác
     * @param status trạng thái cần lọc, null để lấy tất cả
     * @return Tổng hợp doanh thu
     */
    OwnerBookingsSummaryResponse getOwnerBookingsSummary(String ownerEmail, Integer stadiumId, LocalDate startDate, LocalDate endDate, BookingStatus status);
}
