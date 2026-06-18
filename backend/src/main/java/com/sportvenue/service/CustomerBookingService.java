package com.sportvenue.service;

import com.sportvenue.dto.booking.CustomerBookingDetailDto;
import com.sportvenue.dto.booking.CustomerBookingHistoryDto;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.security.UserPrincipal;

public interface CustomerBookingService {
    PageResponse<CustomerBookingHistoryDto> getMyBookings(UserPrincipal principal, String status, int page, int size);
    
    CustomerBookingDetailDto getBookingDetail(UserPrincipal principal, Integer bookingId);
    
    void cancelBooking(UserPrincipal principal, Integer bookingId, String reason);
}
