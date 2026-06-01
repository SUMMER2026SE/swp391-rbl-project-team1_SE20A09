package com.sportvenue.service;

import com.sportvenue.dto.request.RefundRequest;
import com.sportvenue.dto.response.RefundResponse;

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
}
