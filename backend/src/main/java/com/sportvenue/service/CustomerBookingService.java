package com.sportvenue.service;

import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;

public interface CustomerBookingService {
    PageResponse<CustomerBookingHistoryDto> getMyBookings(UserPrincipal principal, int page, int size);
    
    void cancelBooking(UserPrincipal principal, Integer bookingId, String reason);
}
