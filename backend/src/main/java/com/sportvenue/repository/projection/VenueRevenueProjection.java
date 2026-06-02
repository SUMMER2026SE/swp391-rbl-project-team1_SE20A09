package com.sportvenue.repository.projection;

import java.math.BigDecimal;

public interface VenueRevenueProjection {
    Integer getStadiumId();
    String getStadiumName();
    Long getTotalBookings();
    BigDecimal getTotalRevenue();
}
