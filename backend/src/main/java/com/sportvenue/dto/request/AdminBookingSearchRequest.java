package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Tham số filter cho GET /api/v1/admin/bookings — gộp lại thành 1 object
 * để tránh vi phạm Checkstyle ParameterNumber (max 8) trên controller.
 */
@Getter
@Setter
public class AdminBookingSearchRequest {

    @Parameter(description = "Tìm kiếm theo tên, email khách hàng hoặc tên sân")
    private String search;

    @Parameter(description = "Lọc theo trạng thái booking: PENDING_PAYMENT, PENDING, CONFIRMED, COMPLETED, CANCELLED")
    private BookingStatus bookingStatus;

    @Parameter(description = "Lọc theo trạng thái thanh toán: UNPAID, PAID, REFUNDED, DEPOSITED, AWAITING_CASH_PAYMENT")
    private PaymentStatus paymentStatus;

    @Parameter(description = "Ngày bắt đầu chơi (reservationDate)", example = "2026-07-10")
    private LocalDate startDate;

    @Parameter(description = "Ngày kết thúc chơi (reservationDate)", example = "2026-07-11")
    private LocalDate endDate;

    @Parameter(description = "Lọc theo ID sân")
    private Integer stadiumId;

    @Parameter(description = "Lọc theo ID Owner")
    private Integer ownerId;

    @Parameter(description = "Trường sắp xếp", example = "bookingDate")
    private String sortBy = "bookingDate";

    @Parameter(description = "Hướng sắp xếp: asc hoặc desc", example = "desc")
    private String sortDir = "desc";
}
