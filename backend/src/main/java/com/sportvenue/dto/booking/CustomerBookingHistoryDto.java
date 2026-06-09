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
        String status
) {
}
