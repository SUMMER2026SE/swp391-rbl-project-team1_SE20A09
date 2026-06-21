package com.sportvenue.dto.booking;

import java.math.BigDecimal;

public record CustomerBookingHistoryDto(
        String id,
        String displayId,
        String venue,
        String sportType,
        String imageUrl,
        String date,
        String time,
        String location,
        BigDecimal price,
        String status,
        /**
         * UC-CUS-01: Mã chuỗi đặt sân định kỳ — NULL nếu là đơn đơn lẻ.
         * Frontend dùng để hiển thị badge "thuộc chuỗi định kỳ" trong /bookings.
         */
        String recurringGroupId
) {
}