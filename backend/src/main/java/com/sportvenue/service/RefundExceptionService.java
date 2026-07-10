package com.sportvenue.service;

import com.sportvenue.dto.request.AdminDecisionRequest;
import com.sportvenue.dto.request.OwnerReviewRequest;
import com.sportvenue.dto.request.SubmitExceptionRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.RefundExceptionResponse;
import org.springframework.data.domain.Pageable;

/**
 * UC-CUS-1.6 P0: Luồng xét duyệt ngoại lệ hoàn tiền.
 * Khách hủy <12h (0%) xin xem xét lại — Owner duyệt (SLA 48h) → Admin leo thang nếu cần.
 */
public interface RefundExceptionService {

    /**
     * Khách gửi yêu cầu ngoại lệ sau khi hủy đơn nhận 0%.
     * @throws com.sportvenue.exception.BadRequestException nếu điều kiện submit không thỏa
     */
    RefundExceptionResponse submitRequest(String customerEmail, SubmitExceptionRequest req);

    /**
     * Owner duyệt (chấp nhận/từ chối) yêu cầu ngoại lệ.
     * @throws com.sportvenue.exception.ForbiddenException nếu Owner không sở hữu sân liên quan
     * @throws com.sportvenue.exception.BadRequestException nếu business rule approved/refundPercent vi phạm
     */
    RefundExceptionResponse ownerReview(String ownerEmail, Integer requestId, OwnerReviewRequest req);

    /**
     * Khách leo thang lên Admin sau khi Owner từ chối.
     * @throws com.sportvenue.exception.BadRequestException nếu status không phải REJECTED_OWNER
     */
    RefundExceptionResponse customerEscalate(String customerEmail, Integer requestId);

    /**
     * Admin ra quyết định cuối (chấp nhận/từ chối).
     * @throws com.sportvenue.exception.BadRequestException nếu business rule approved/refundPercent vi phạm
     */
    RefundExceptionResponse adminDecide(String adminEmail, Integer requestId, AdminDecisionRequest req);

    /** Khách xem danh sách yêu cầu ngoại lệ của mình. */
    PageResponse<RefundExceptionResponse> getCustomerRequests(String customerEmail, Pageable pageable);

    /** Owner xem danh sách yêu cầu đang chờ liên quan sân của mình. */
    PageResponse<RefundExceptionResponse> getOwnerPendingRequests(String ownerEmail, Pageable pageable);

    /** Admin xem toàn bộ hàng đợi PENDING_ADMIN. */
    PageResponse<RefundExceptionResponse> getAdminQueue(Pageable pageable);

    /** Lấy request mới nhất của 1 booking (để hiển thị badge trên UI booking detail). */
    RefundExceptionResponse getLatestByBookingId(Integer bookingId);
}
