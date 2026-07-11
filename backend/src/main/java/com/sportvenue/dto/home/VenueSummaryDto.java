package com.sportvenue.dto.home;

import java.math.BigDecimal;

public record VenueSummaryDto(
        Integer id,
        String name,
        String complexName,
        String sportType,
        String sportKey,
        BigDecimal pricePerHour,
        BigDecimal rating,
        long reviewCount,
        String location,
        String imageUrl,
        boolean saved
) {
}
