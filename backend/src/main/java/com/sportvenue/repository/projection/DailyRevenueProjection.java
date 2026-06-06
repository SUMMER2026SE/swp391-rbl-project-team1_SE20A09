package com.sportvenue.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueProjection {
    LocalDate getDate();

    BigDecimal getRevenue();
}
