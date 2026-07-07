package com.sportvenue.service;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.OwnerBookingResponse;
import com.sportvenue.dto.response.RefundResponse;

import java.util.List;

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
     * @param ownerEmail Email của Owner đang thực hiện thao tác
     * @return Kết quả hoàn tiền ước tính chi tiết
     */
    RefundResponse previewRefund(Integer bookingId, String ownerEmail);

    /**
     * Lấy trạng thái/kết quả hoàn tiền hiện tại của một đơn (dùng cho idempotency replay).
     *
     * @param bookingId ID của đơn đặt sân
     * @param ownerEmail Email của Owner
     * @return Kết quả hoàn tiền chi tiết
     */
    RefundResponse getRefundResponse(Integer bookingId, String ownerEmail);

    /**
     * Lấy toàn bộ danh sách đặt sân của Owner để hiển thị trên Dashboard.
     *
     * @param ownerEmail Email của Owner đang thực hiện thao tác
     * @return Danh sách DTO chứa thông tin đặt sân
     */
    List<OwnerBookingResponse> getOwnerBookings(String ownerEmail);
}
