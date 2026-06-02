package com.sportvenue.repository.projection;

import java.math.BigDecimal;

public interface DailyRevenueProjection {
    java.time.LocalDate getDate();
    BigDecimal getRevenue();
}
